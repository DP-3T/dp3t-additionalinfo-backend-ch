# SwissCovid Additional Info Backend

SwissCovid is the official contact tracing app of Switzerland. The app can be installed from the [App Store](https://apps.apple.com/ch/app/swisscovid/id1509275381) or the [Google Play Store](https://play.google.com/store/apps/details?id=ch.admin.bag.dp3t). The SwissCovid 2.0 app uses two types of contact tracing to prevent the spread of COVID-19.

With proximity tracing close contacts are detected using the bluetooth technology. For this the [dp3t-sdk-backend](https://github.com/DP-3T/dp3t-sdk-backend), [DP3T iOS SDK](https://github.com/DP-3T/dp3t-sdk-ios) and [DP3T Android SDK](https://github.com/DP-3T/dp3t-sdk-android) are used that build on top of the Google & Apple Exposure Notifications. This feature is called SwissCovid encounters.

With presence tracing people that are at the same venue at the same time are detected. For this the [swisscovid-cn-backend](https://github.com/SwissCovid/swisscovid-cn-backend), [CrowdNotifier iOS SDK](https://github.com/CrowdNotifier/crowdnotifier-sdk-ios) and [CrowdNotifier Android SDK](https://github.com/CrowdNotifier/crowdnotifier-sdk-android) are used that provide a secure, decentralized, privacy-preserving presence tracing system. This feature is called SwissCovid Check-in.

## Description

The [Swisscovid Additional Info Backend](https://github.com/SwissCovid/swisscovid-additionalinfo-backend) is used by the apps to fetch and refresh the statistical data displayed under the "Stats" tab.

## Repositories

- Android App: [swisscovid-app-android](https://github.com/SwissCovid/swisscovid-app-android)
- iOS App: [swisscovid-app-ios](https://github.com/SwissCovid/swisscovid-app-ios)
- CovidCode Web-App: [CovidCode-UI](https://github.com/admin-ch/CovidCode-UI)
- CovidCode Backend: [CovidCode-Service](https://github.com/admin-ch/CovidCode-service)
- Config Backend: [swisscovid-config-backend](https://github.com/SwissCovid/swisscovid-config-backend)
- Additional Info Backend: [swisscovid-additionalinfo-backend](https://github.com/SwissCovid/swisscovid-additionalinfo-backend)
- QR Code Landingpage: [swisscovid-qr-landingpage](https://github.com/SwissCovid/swisscovid-qr-landingpage)
- DP3T Android SDK & Calibration app: [dp3t-sdk-android](https://github.com/DP-3T/dp3t-sdk-android)
- DP3T iOS SDK & Calibration app: [dp3t-sdk-ios](https://github.com/DP-3T/dp3t-sdk-ios)
- DP3T Backend SDK: [dp3t-sdk-backend](https://github.com/DP-3T/dp3t-sdk-backend)
- CrowdNotifier Android SDK: [crowdnotifier-sdk-android](https://github.com/CrowdNotifier/crowdnotifier-sdk-android)
- CrowdNotifier iOS SDK: [crowdnotifier-sdk-ios](https://github.com/CrowdNotifier/crowdnotifier-sdk-ios)
- CrowdNotifier Backend: [swisscovid-cn-backend](https://github.com/SwissCovid/swisscovid-cn-backend)

## License

This project is licensed under the terms of the MPL 2 license. See the [LICENSE](LICENSE) file.
