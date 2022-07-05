# Changelog for the OpenAPI Validator for Axway API-Management
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [1.6.3] 2022-07-05
### Fixed
- NPE when validating method with query parameters without providing the correct query parameter as part of the request

## [1.6.2] 2022-06-30
### Fixed
- If a Content-Type header is set, it is used for validation and then removed, because it additionally exists in the http.content.headers attribute.

## [1.6.1] 2022-06-27
### Fixed
- Added call to removeDuplicateContentTypeHeader when return header to validator

## [1.6.0] 2022-06-24
### Added
- Now query parameters are by default decoded before send to the validator (e.g. otr%C3%B3s -> otrós)

### Changed
- Dropped support for Java-Version 1.7. Minimal Java-Version is 1.8

## [1.5.0] 2022-06-23
### Added
- If the Content-Type header is duplicated, only the first header is taken and all others are deleted.

## [1.4.1] 2022-06-09
### Changed
- If Content-Type header could not be found is now logged on DEBUG instead of DATA [#7](https://github.com/Axway-API-Management-Plus/openapi-validator/issues/7)

## [1.4.0] 2022-03-28
### Fixed
- Making access to the validation report documented and easier [#5](https://github.com/Axway-API-Management-Plus/openapi-validator/issues/5)

## [1.3.0] 2022-01-10
### Fixed
- Unexpected log message java exception no longer appears

### Added
- Add an option to use the originally imported API-Specification for the validation [#4](https://github.com/Axway-API-Management-Plus/openapi-validator/issues/4)

## [1.2.0] 2021-12-21
### Fixed
- StringIndexOutOfBoundsException while search matching path [#3](https://github.com/Axway-API-Management-Plus/openapi-validator/issues/3)

## [1.1.0] 2021-12-21
### Fixed
- Failed to validate API-Request using a path parameter [#1](https://github.com/Axway-API-Management-Plus/openapi-validator/issues/1)

### Added
- Given path variable should support the full exposure path [#2](https://github.com/Axway-API-Management-Plus/openapi-validator/issues/2)

## [1.0.0] 2021-06-22

- Initial release
