# Bluetooth 6.0 Channel Sounding Ranging Dataset (Pixel 10 Pro)

## 📖 Overview

This dataset accompanies our paper **"Beyond Proximity: Evaluating High-Precision Bluetooth Channel Sounding for Mobile Applications"** (submitted to *IPIN 2026*). It contains the complete set of ranging measurements collected with a commercial *smartphone (Google Pixel 10 Pro)* acting as the *Initiator* and a Nordic Semiconductor *nRF54L15* Development Kit acting as the *Reflector*, communicating via the Bluetooth 6.0 **Channel Sounding (CS)** protocol.

This dataset provides Bluetooth CS ranging measurements captured with a commercial off-the-shelf smartphone and covers static and dynamic scenarios in indoor and outdoor environments. The dataset is accompanied by timestamped UWB ground-truth measurements and analysis scripts.

## 🛰️ Experimental Setup

| Component | Specification |
| :--- | :--- |
| Initiator | Google Pixel 10 Pro (Android 16, BT 6.0 HAL) |
| Reflector | Nordic Semiconductor nRF54L15 DK |
| Ground Truth | Qorvo DW3000 UWB modules (mechanically co-located with BT antennas) |
| Frequency Band | 2.4 GHz ISM, up to 72 channels |
| Update Rates | 0.2 Hz, 5 Hz, 10 Hz |
| Environments | Outdoor grassland (60 m × 30 m, LOS) / Indoor corridor (2.5 m wide, multipath-rich) |

## 📁 Repository Structure

```
dataset/
├── GT/                          # UWB ground-truth distance measurements
│   ├── Static_*.csv
│   ├── Dynamic_*.csv
│   └── vis.py                   # Visualization script
└── RangingFilter/               # Timestamped Android Bluetooth CS results
    ├── Static_*.csv
    └── Dynamic_*.csv
```

- **`GT/`** — contains the reference distances measured by the co-located Qorvo DW3000 UWB system.
- **`RangingFilter/`** — contains the application-level Bluetooth CS results returned by the Android API after duplicate timestamps were removed.
- A CS file and its corresponding UWB ground-truth file use the same scenario filename. The analysis scripts align the two files by nearest timestamp with a maximum tolerance of 500 ms.
- **`vis.py`** — visualizes the UWB ground-truth traces.

### File Naming Convention

Files follow the pattern:

```
<Mode>_<UpdateRate>_<Distance|Pattern>_<Environment>.csv
```

| Field | Possible Values |
| :--- | :--- |
| `Mode` | `Static`, `Dynamic` |
| `UpdateRate` | `02Hz` (0.2 Hz), `5Hz`, `10Hz` |
| `Distance` (static) | `1m`, `10m`, `25m`, `50m` |
| `Pattern` (dynamic) | *(none — linear walk)*, `Backforth`, `Swing` |
| `Environment` | `Outdoor`, `Indoor_Corridor` |

**Examples:**
- `Static_5Hz_10m_Outdoor.csv` — 10 m static measurement, 5 Hz update, outdoor LOS.
- `Dynamic_10Hz_Outdoor_Swing.csv` — Dynamic walk on a rectangular path with natural arm swing, 10 Hz, outdoor.
- `Dynamic_5Hz_Indoor_Corridor.csv` — Dynamic walk in indoor corridor at 5 Hz.

## 📊 Data Format

### Bluetooth CS files (`RangingFilter/`)

| Column | Description | Unit/values |
| --- | --- | --- |
| `Timestamp` | System timestamp associated with the API result | Date and time with millisecond resolution |
| `Device Name` | Name of the Bluetooth CS reflector | Text |
| `Address` | Bluetooth address recorded during the experiment | Text |
| `Distance (m)` | Processed distance estimate returned by the Android Channel Sounding API | m |
| `Confidence Level` | Categorical confidence returned by the Android API | `0`: low, `1`: medium, `2`: high |

The Android API used in this measurement campaign exposed the timestamp, distance estimate, and categorical confidence level. It did not expose raw IQ samples, per-tone phase measurements, or a numerical distance uncertainty/standard deviation. The confidence level is therefore not interpreted as a calibrated error probability.

### UWB ground-truth files (`GT/`)

| Column | Description | Unit |
| --- | --- | --- |
| `Timestamp` | Timestamp of the UWB reference measurement | Date and time with millisecond resolution |
| `Distance(m)` | Ground-truth distance measured using the Qorvo DW3000 system | m |

Bluetooth CS and UWB data are intentionally stored in separate files. Files belonging to the same experiment have identical scenario filenames. During evaluation, each Bluetooth CS result is associated with the nearest UWB timestamp within a maximum tolerance of 500 ms.

> ⚠️ Some dynamic scenarios contain occasional `NaN` rows corresponding to dropped CS subevents or lost links (especially at 50 m where SNR approaches the Bluetooth connectivity threshold).

## 🧪 Scenario Matrix

### Static Scenarios

| Distance | Outdoor (5 Hz) | Outdoor (10 Hz) | Outdoor (0.2 Hz) | Indoor (5 Hz) |
| :--- | :---: | :---: | :---: | :---: |
| 1 m  | ✅ | ✅ | — | ✅ |
| 10 m | ✅ | ✅ | ✅ | ✅ |
| 25 m | ✅ | ✅ | — | ✅ |
| 50 m | ✅ | ✅ | — | ✅ |

### Dynamic Scenarios

| Pattern | Outdoor (5 Hz) | Outdoor (10 Hz) | Indoor (5 Hz) | Indoor (10 Hz) |
| :--- | :---: | :---: | :---: | :---: |
| Handheld linear walk        | ✅ | ✅ | ✅ | ✅ |
| Back & Forth (180° turn)    | ✅ | ✅ | — | — |
| Natural Arm Swing (rect.)   | ✅ | ✅ | — | — |

## 🖼️ Dataset Overview

The figure below provides a holistic visualization of all 21 ranging traces in the dataset (both raw and filtered), plotted against time. It can be regenerated using `GT/vis.py`.

![Dataset Overview](./overview.png)

## 📚 Citation

If you use this dataset, please cite our paper (to appear at **IPIN 2026**):

```bibtex
@inproceedings{xu2026ipin,
  title     = {From Proximity to Precision: An Empirical Evaluation of
               Bluetooth Channel Sounding on Smartphones},
  author    = {Ruijie Xu, Penghui Xu, Cheng Chang, Tung-Hao Hsu, and Li-Ta Hsu},
  booktitle = {Proc. International Conference on Indoor Positioning and
               Indoor Navigation (IPIN)},
  year      = {2026},
  note      = {To appear}
}
```

## 📜 License

This dataset is released under the **Creative Commons Attribution 4.0 International (CC BY 4.0)** license. You are free to share and adapt the material for any purpose, provided proper attribution is given.

## ✉️ Contact

For questions, bug reports, or collaboration inquiries, please contact ruijie.xu@connect.polyu.hk and pengh.xu@polyu.edu.hk.

