# Dependencies and licenses

Dependency versions are declared in [`app/build.gradle`](../app/build.gradle).

| Dependency | Purpose | License |
| --- | --- | --- |
| [Bouncy Castle Java](https://github.com/bcgit/bc-java) (`bcprov-jdk18on`) | Argon2id verification | [Bouncy Castle (MIT-style)](../app/src/main/assets/licenses/BOUNCY-CASTLE.txt) |
| [AndroidX ExifInterface](https://developer.android.com/jetpack/androidx/releases/exifinterface) | Image orientation metadata | [Apache License 2.0](../app/src/main/assets/licenses/APACHE-2.0.txt) |
| JUnit | Unit tests only; not bundled in the release APK | Eclipse Public License 1.0 |
| Android SDK and Android Gradle Plugin | Platform APIs and build tooling | Their respective distribution licenses |

The two runtime dependency licenses are included in the source tree under
`app/src/main/assets/licenses/` and are packaged into release APKs. Bouncy
Castle also embeds its license at `META-INF/LICENSE.md` in the APK.

Neuro Granny's source code is distributed under the MIT license; see
[`LICENSE`](../LICENSE). Third-party components retain their own copyright
notices and license terms.

The app does not request Android's `INTERNET` permission. Its backups are
user-initiated; an external document provider may offer cloud storage.
