#### Features
- Introduced `MapboxSpeedInfoApi` and `MapboxSpeedInfoView`. The combination of API and View can be used to render posted and current speed limit at user's current location.
- :warning: Deprecated `MapboxSpeedLimitApi` and `MapboxSpeedLimitView`.

#### Bug fixes and improvements
- Fixed approaches list update in `RouteOptionsUpdater`(uses for reroute). It was putting to the origin approach corresponding approach from legacy approach list.
- Updated the `MapboxRestAreaApi` logic to load a SAPA map only if the upcoming rest stop is at the current step of the route leg.

#### Known issues :warning:
- It is an example of known issues