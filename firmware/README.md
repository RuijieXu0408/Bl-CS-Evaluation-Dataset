# Reflector firmware (nRF54L15 DK)

Bluetooth LE Channel Sounding **reflector** with the Ranging Responder (RAS) role, for
the Nordic Semiconductor nRF54L15 DK. It is the peer of the Android initiator app in
[`../app`](../app).

This is a modified copy of the `channel_sounding_ras_reflector` sample from the
nRF Connect SDK (`nrf/samples/bluetooth/channel_sounding_ras_reflector`, SDK v3.1.1).
`README.rst` is the original sample documentation from Nordic Semiconductor.

> **Version note.** The measurements in this dataset were collected in January 2026
> with an earlier revision of this firmware. The code published here is the current
> revision, which adds the reliability fixes listed below; it is not byte-identical to
> the build used for the paper.

## What was changed from the stock sample

| Change | Where | Why |
|---|---|---|
| Bonds are kept across boots | `src/main.c` | Clearing bonds on boot invalidates the key stored on the phone, so every reconnection fails encryption (HCI `0x06`, key missing) and ranging never starts. |
| Advertising restarts after a disconnect instead of a cold reboot | `src/main.c` | A reboot on every disconnect discards all controller state and makes the next session unreliable. |
| Re-pairing allowed, 4 bond slots | `rebonding.conf` | Lets a phone that has forgotten its bond pair again instead of being rejected. |
| 2 antennas / 4 antenna paths | `antenna2.conf` | Matches the antenna configuration Android selects. |
| Connection event length raised to 30 ms; automatic PHY update disabled | `cs_scheduling.conf` | `CONFIG_BT_AUTO_PHY_UPDATE=n` (taken from the v3.4.0 sample) is what made CS Procedure Enable succeed reliably. |
| Distinct advertising name for a second board | `name_b.conf` | Optional; only for running two reflectors. |

`android_ranging.conf` is the fragment Nordic provides for ranging with Android 16 phones.
`debug_ras.conf` enables verbose logging and is not part of the normal build.

## Building

Requires nRF Connect SDK **v3.1.1** and its toolchain. With the SDK environment active:

```
west build -b nrf54l15dk/nrf54l15/cpuapp -- -DEXTRA_CONF_FILE="android_ranging.conf;antenna2.conf;rebonding.conf;cs_scheduling.conf"
west flash
```

For a second reflector, append `;name_b.conf` to the fragment list and use a separate
build directory (`--build-dir build_b`).

The board advertises as `Nordic CS Reflector A` (or `B`). Pair it from the app on first
connection; the bond is stored on the board and survives power cycles. Note that
re-flashing erases the stored bond, after which the phone must forget the device and
pair again.

nRF Connect SDK v3.4.0 was also tried and did not work with the Pixel 10 Pro
(Android 16) used here; v3.1.1 is the version this firmware is known to work with.

## License

Nordic 5-Clause (`LicenseRef-Nordic-5-Clause`), the license of the original sample; see
[`LICENSE`](LICENSE). In particular, this software may only be used with a Nordic
Semiconductor integrated circuit. The modifications are released under the same terms.
This is not an official Nordic Semiconductor release.
