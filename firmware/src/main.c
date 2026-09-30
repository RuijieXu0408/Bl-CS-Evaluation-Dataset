/*
 * Copyright (c) 2024 Nordic Semiconductor ASA
 *
 * SPDX-License-Identifier: LicenseRef-Nordic-5-Clause
 */

/** @file
 *  @brief Channel Sounding Reflector with Ranging Responder sample
 */

#include <zephyr/types.h>
#include <zephyr/kernel.h>
#include <zephyr/bluetooth/bluetooth.h>
#include <zephyr/sys/reboot.h>
#include <zephyr/bluetooth/conn.h>
#include <zephyr/bluetooth/uuid.h>
#include <zephyr/bluetooth/cs.h>
#include <bluetooth/services/ras.h>
#include <zephyr/settings/settings.h>
#include <dk_buttons_and_leds.h>

#include <zephyr/logging/log.h>
LOG_MODULE_REGISTER(app_main, LOG_LEVEL_INF);

#define CON_STATUS_LED DK_LED1

static K_SEM_DEFINE(sem_connected, 0, 1);

static struct bt_conn *connection;

static const struct bt_data ad[] = {
	BT_DATA_BYTES(BT_DATA_FLAGS, (BT_LE_AD_GENERAL | BT_LE_AD_NO_BREDR)),
	BT_DATA_BYTES(BT_DATA_UUID16_ALL, BT_UUID_16_ENCODE(BT_UUID_RANGING_SERVICE_VAL)),
	BT_DATA(BT_DATA_NAME_COMPLETE, CONFIG_BT_DEVICE_NAME, sizeof(CONFIG_BT_DEVICE_NAME) - 1),
};

static void adv_restart_work_handler(struct k_work *work)
{
	ARG_UNUSED(work);

	int err = bt_le_adv_start(BT_LE_ADV_CONN_FAST_2, ad, ARRAY_SIZE(ad), NULL, 0);

	if (err == -EALREADY) {
		return;
	} else if (err) {
		LOG_ERR("Advertising failed to restart (err %d)", err);
	} else {
		LOG_INF("Advertising restarted, device is discoverable again.");
	}
}

static K_WORK_DEFINE(adv_restart_work, adv_restart_work_handler);

static void connected_cb(struct bt_conn *conn, uint8_t err)
{
	char addr[BT_ADDR_LE_STR_LEN];

	(void)bt_addr_le_to_str(bt_conn_get_dst(conn), addr, sizeof(addr));
	LOG_INF("Connected to %s (err 0x%02X)", addr, err);

	if (err) {
		bt_conn_unref(conn);
		connection = NULL;
	}

	connection = bt_conn_ref(conn);

	k_sem_give(&sem_connected);

	dk_set_led_on(CON_STATUS_LED);
}

static void disconnected_cb(struct bt_conn *conn, uint8_t reason)
{
	LOG_INF("Disconnected (reason 0x%02X)", reason);

	bt_conn_unref(conn);
	connection = NULL;

	dk_set_led_off(CON_STATUS_LED);

	/* Restart advertising instead of rebooting.
	 *
	 * This used to call sys_reboot(SYS_REBOOT_COLD). A cold reboot tears down the
	 * whole controller on every disconnect, which loses all Channel Sounding state
	 * and makes the next session flaky: the initiator reuses its cached CS config
	 * for a controller that has just been reset, and CS Procedure Enable then fails
	 * with UNSUPPORTED_LMP_OR_LL_PARAMETER (0x20).
	 *
	 * bt_le_adv_start() must not be called directly from this callback: the
	 * connection object is still being released, so it can fail with -EAGAIN.
	 * Submit it to the system work queue instead.
	 */
	k_work_submit(&adv_restart_work);
}

static void remote_capabilities_cb(struct bt_conn *conn,
				   uint8_t status,
				   struct bt_conn_le_cs_capabilities *params)
{
	ARG_UNUSED(conn);
	ARG_UNUSED(params);

	if (status == BT_HCI_ERR_SUCCESS) {
		LOG_INF("CS capability exchange completed.");
	} else {
		LOG_WRN("CS capability exchange failed. (HCI status 0x%02x)", status);
	}
}

static void config_create_cb(struct bt_conn *conn,
			      uint8_t status,
			      struct bt_conn_le_cs_config *config)
{
	ARG_UNUSED(conn);

	if (status == BT_HCI_ERR_SUCCESS) {
		LOG_INF("CS config creation complete. ID: %d", config->id);
	} else {
		LOG_WRN("CS config creation failed. (HCI status 0x%02x)", status);
	}
}

static void security_enable_cb(struct bt_conn *conn, uint8_t status)
{
	ARG_UNUSED(conn);

	if (status == BT_HCI_ERR_SUCCESS) {
		LOG_INF("CS security enabled.");
	} else {
		LOG_WRN("CS security enable failed. (HCI status 0x%02x)", status);
	}
}

static void procedure_enable_cb(struct bt_conn *conn,
				uint8_t status,
				struct bt_conn_le_cs_procedure_enable_complete *params)
{
	ARG_UNUSED(conn);

	if (status == BT_HCI_ERR_SUCCESS) {
		if (params->state == 1) {
			LOG_INF("CS procedures enabled.");
		} else {
			LOG_INF("CS procedures disabled.");
		}
	} else {
		LOG_WRN("CS procedures enable failed. (HCI status 0x%02x)", status);
	}
}

BT_CONN_CB_DEFINE(conn_cb) = {
	.connected = connected_cb,
	.disconnected = disconnected_cb,
	.le_cs_read_remote_capabilities_complete = remote_capabilities_cb,
	.le_cs_config_complete = config_create_cb,
	.le_cs_security_enable_complete = security_enable_cb,
	.le_cs_procedure_enable_complete = procedure_enable_cb,
};

int main(void)
{
	int err;

	LOG_INF("Starting Channel Sounding Reflector Sample");

	dk_leds_init();
	dk_set_led_on(DK_LED2); // 一开机就点亮 LED1
	dk_set_led_on(DK_LED3);
	dk_set_led_on(DK_LED4);
    // k_sleep(K_FOREVER);     // 挂起
	LOG_INF("Starting Channel Sounding Reflector Sample");
	err = bt_enable(NULL);
	if (err) {
		LOG_ERR("Bluetooth init failed (err %d)", err);
		return 0;
	}

	if (IS_ENABLED(CONFIG_SETTINGS)) {
        settings_load();
		LOG_INF("Settings loaded successfully");
    }

	/* Do NOT clear bonds on boot.
	 *
	 * Android keeps its LTK for this reflector across app restarts. If we wipe
	 * our side of the bond here, the phone reconnects and encrypts with a key we
	 * no longer have, the controller answers "PIN or Key Missing" (HCI 0x06) and
	 * Android drops the link (logcat: encryption_change:key_missing). CS security
	 * needs an encrypted link, so ranging never starts. android_ranging.conf
	 * enables CONFIG_BT_BONDABLE/CONFIG_BT_SETTINGS precisely so the bond
	 * survives; settings_load() above restores it from NVS.
	 */

	// 2. 检查是否真的加载到了 ID
	bt_addr_le_t addrs[1];
	size_t count = 1;
	bt_id_get(addrs, &count);

	if (count == 0) {
		// 如果没有 ID，则手动创建一个，这会消除 "No ID address" 警告
		LOG_INF("No ID found, creating a new permanent ID...");
		err = bt_id_create(NULL, NULL);
		if (err < 0) {
			LOG_ERR("Failed to create ID (err %d)", err);
		}
		
		// 3. 强制保存到 Flash
		if (IS_ENABLED(CONFIG_SETTINGS)) {
			settings_save();
		}
	}

	LOG_INF("Settings loaded successfully. Identity ready.");

	k_sleep(K_MSEC(500));
	err = bt_le_adv_start(BT_LE_ADV_CONN_FAST_2, ad, ARRAY_SIZE(ad), NULL, 0);
	if (err) {
		LOG_ERR("Advertising failed to start (err %d)", err);
		return 0;
	}

	while (true) {
		k_sem_take(&sem_connected, K_FOREVER);

		const struct bt_le_cs_set_default_settings_param default_settings = {
			.enable_initiator_role = false,
			.enable_reflector_role = true,
			.cs_sync_antenna_selection = BT_LE_CS_ANTENNA_SELECTION_OPT_REPETITIVE,
			.max_tx_power = BT_HCI_OP_LE_CS_MAX_MAX_TX_POWER,
		};

		err = bt_le_cs_set_default_settings(connection, &default_settings);
		if (err) {
			LOG_ERR("Failed to configure default CS settings (err %d)", err);
		}
	}

	return 0;
}
