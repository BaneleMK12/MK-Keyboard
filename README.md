# MK Keyboard

MK Keyboard is a simple Android input method with a cool blue interface and a
compact QWERTY layout. It is built with Kotlin and Android's
`InputMethodService`, so it works as a real system keyboard instead of only
being a visual demo.

## Package

`com.mkdev.mkkeyboard`

## Using the app

1. Install the APK.
2. Open MK Keyboard and tap **Enable MK Keyboard**.
3. Turn on MK Keyboard in Android's keyboard settings.
4. Tap **Choose MK Keyboard** and select MK Keyboard.

The keyboard includes a GIF button above the letter rows. It loads trending GIFs
from GIPHY, supports searching, and sends a selected GIF directly to apps that
support Android rich-content input. A GIPHY URL is inserted as a fallback for
older or unsupported text fields. The GIF picker displays the required
Powered by GIPHY attribution.

## Building

The GitHub Actions workflow builds the debug APK on every push and pull
request. Android SDK and Java are provisioned by GitHub Actions.