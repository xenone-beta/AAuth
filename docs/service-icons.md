# Service icons

AAuth colors the avatar of well-known services with their brand color (GitHub, Google, Discord, ...).
Real logos are not bundled, because brand artwork has its own license terms. You can add the ones you want:

1. Get a monochrome SVG of the logo, for example from [Simple Icons](https://simpleicons.org) (CC0 files,
   but the brands themselves are trademarks of their owners).
2. Convert it to an Android vector drawable: in Android Studio use **New, Vector Asset, Local file (SVG)**.
3. Save it as `app/src/main/res/drawable/svc_<slug>.xml`, where `<slug>` is the name from the list in
   `ServiceAvatar.kt` (`github`, `google`, `discord`, `telegram`, ...). Example: `svc_github.xml`.
4. Rebuild. The logo is drawn in white (or black on light brand colors) on the brand-colored circle.

Services without a logo drawable keep the colored circle with the first letter.
To add a new service, add a line to the `brands` list in `ServiceAvatar.kt`.
