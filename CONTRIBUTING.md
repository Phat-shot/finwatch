# Contributing to Finwatch

Thanks for helping out! This is a small project — the rules are short.

## Branch model

- `test` — default branch, all development happens here. **Open pull
  requests against `test`.**
- `main` — release branch. Releases are cut by merging `test` into `main`;
  don't target it directly.

## Building

- JDK 17
- Android SDK with **compileSdk 36**
- Gradle comes from the wrapper (8.14, with AGP 8.13 / Kotlin 2.3)

```bash
./gradlew assembleBetaRelease   # Finwatch beta — the flavor built from test
./gradlew assembleProdRelease   # Finwatch (prod)
```

No signing setup is needed: without release secrets/properties, builds fall
back to the committed debug keystore and are installable for sideloading.
See "Local build" and "Release signing" in the [README](README.md).

## Pull request expectations

- CI (`.github/workflows/build.yml`) runs a **validate** job — Android Lint
  and unit tests — on every PR. It must be green before a merge; run
  `./gradlew lint test` locally to check ahead of time. Note that missing
  string translations are a lint error: every new string resource needs
  entries in all five locales (`values`, `values-de`, `values-fr`,
  `values-es`, `values-ar`).
- **Update [CHANGELOG.md](CHANGELOG.md)**: every PR that changes
  user-visible behavior adds a line to the `[Unreleased]` section
  (Keep-a-Changelog categories: Added / Changed / Fixed / Security / …).
- Keep commits small and thematic; explain the *why* in the message body.

## License

Finwatch is licensed under the
[Mozilla Public License 2.0](LICENSE). By contributing you agree that your
contributions are licensed under the same terms (MPL-2.0). Licenses of
bundled third-party components are tracked in
[THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).
