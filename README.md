# SIO2BT
SIO2BT Android App

SIO2BT project consist of hardware and software related to the wireless Bluetooth communication between the 8-bit Atari computers and emulated SIO devices.

SIO2BT App requires a Bluetooth hardware extension for the ATARI (HC-06 Transceiver).
The device name has to start with "SIO2BT" or with "ATARI".
You can contact montezuma@abbuc.de to get more detials.

Documentation and software:
https://drive.google.com/file/d/0B3-191R-U_S1blpUTFBsRW1iRUE

The SIO2BT App emulates up to 4 floppy disks.
You can select disk images (*.atr) and executable files (*.xex, *.com, *.exe).
A "long touch" on an executable allows you to choose the xex loader address (default value is $700).
Write protection mode (R/RW) for a disk image can be set with a "long touch", too.

The emulated disks (unless write protected) can be modified (SIO commands: format, write sector, etc. are supported).

SIO2BT app supports also a new SIO Networking Device ($4E) and a Smart Device ($45).
Networking and Smart Devices are not activated per default (and can be enabled in the App Settings).

Many thanks to:
Bernd, Bob!k, Dietrich, drac030, FlashJazzCat, Greblus, HardwareDoc, Hias, Igor Gramblička, Kr0tki, Lotharek, mr-atari, Tom Hudson, TRUB, xxl
