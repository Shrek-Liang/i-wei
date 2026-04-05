//
//  ContentView.swift
//  iOS_weather
//
//  Created by C Doctor on 4/4/26.
//

import CoreLocation
import Observation
import SwiftUI

struct ContentView: View {
    @State private var viewModel = WeatherViewModel()

    var body: some View {
        ZStack {
            backgroundGradient

            ScrollView(showsIndicators: false) {
                VStack(spacing: 18) {
                    headerSection

                    if let weather = viewModel.weather {
                        hourlyForecastSection(weather: weather)
                        detailGrid(weather: weather)
                        dailyForecastSection(weather: weather)
                    } else {
                        statusSection
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 24)
                .padding(.bottom, 40)
            }
            .refreshable {
                await viewModel.refreshWeather()
            }
        }
        .task {
            await viewModel.loadWeatherIfNeeded()
        }
    }

    private var backgroundGradient: some View {
        ZStack {
            LinearGradient(
                colors: viewModel.theme.gradientColors,
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()

            Circle()
                .fill(.white.opacity(0.10))
                .frame(width: 280, height: 280)
                .blur(radius: 12)
                .offset(x: 120, y: -280)
        }
    }

    private var headerSection: some View {
        VStack(spacing: 10) {
            HStack {
                Spacer()

                Button {
                    Task {
                        await viewModel.refreshWeather()
                    }
                } label: {
                    Image(systemName: "arrow.clockwise")
                        .font(.headline.weight(.semibold))
                        .foregroundStyle(.white)
                        .frame(width: 40, height: 40)
                        .background(.white.opacity(0.16), in: Circle())
                }
                .buttonStyle(.plain)
            }

            Text(viewModel.cityName)
                .font(.system(size: 34, weight: .medium, design: .rounded))
                .foregroundStyle(.white)

            if let weather = viewModel.weather {
                Text("\(weather.current.temperature)°")
                    .font(.system(size: 96, weight: .thin, design: .rounded))
                    .foregroundStyle(.white)
                    .padding(.top, -8)

                Text(weather.current.description)
                    .font(.title3.weight(.semibold))
                    .foregroundStyle(.white.opacity(0.96))

                Text("H:\(weather.today.high)°  L:\(weather.today.low)°")
                    .font(.headline)
                    .foregroundStyle(.white.opacity(0.78))
            } else {
                Text(viewModel.isLoading ? "Loading..." : "--°")
                    .font(.system(size: 96, weight: .thin, design: .rounded))
                    .foregroundStyle(.white)
                    .padding(.top, -8)

                Text(viewModel.statusText)
                    .font(.title3.weight(.semibold))
                    .foregroundStyle(.white.opacity(0.92))
                    .multilineTextAlignment(.center)

                if let message = viewModel.errorMessage {
                    Text(message)
                        .font(.footnote)
                        .foregroundStyle(.white.opacity(0.76))
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 18)
                }
            }
        }
        .padding(.vertical, 18)
    }

    private var statusSection: some View {
        WeatherCard {
            VStack(alignment: .leading, spacing: 14) {
                Label(viewModel.statusText, systemImage: viewModel.isLoading ? "location.fill" : "exclamationmark.triangle.fill")
                    .font(.headline)
                    .foregroundStyle(.white)

                Text(viewModel.errorMessage ?? "Pull down to refresh after granting location access.")
                    .font(.subheadline)
                    .foregroundStyle(.white.opacity(0.80))

                if viewModel.isLoading {
                    ProgressView()
                        .tint(.white)
                }
            }
        }
    }

    private func hourlyForecastSection(weather: WeatherSnapshot) -> some View {
        WeatherCard {
            VStack(alignment: .leading, spacing: 14) {
                Label(weather.headline, systemImage: "clock")
                    .font(.footnote.weight(.medium))
                    .foregroundStyle(.white.opacity(0.82))

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 18) {
                        ForEach(weather.hourlyForecasts) { forecast in
                            VStack(spacing: 10) {
                                Text(forecast.timeLabel)
                                    .font(.caption.weight(.semibold))
                                    .foregroundStyle(.white.opacity(0.82))

                                Image(systemName: forecast.symbolName)
                                    .font(.title3)
                                    .symbolRenderingMode(.multicolor)

                                Text("\(forecast.temperature)°")
                                    .font(.headline.weight(.medium))
                                    .foregroundStyle(.white)
                            }
                            .frame(width: 56)
                        }
                    }
                    .padding(.vertical, 2)
                }
            }
        }
    }

    private func detailGrid(weather: WeatherSnapshot) -> some View {
        LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 14), count: 2), spacing: 14) {
            WeatherMetricCard(title: "HUMIDITY", value: "\(weather.current.humidity)%", detail: weather.humidityDescription, systemImage: "humidity.fill")
            WeatherMetricCard(title: "UV INDEX", value: weather.uvIndexText, detail: weather.uvDescription, systemImage: "sun.max.fill")
            WeatherMetricCard(title: "SUN", value: weather.sunPrimary, detail: weather.sunSecondary, systemImage: "sunset.fill")
            WeatherMetricCard(title: "WIND", value: "\(weather.current.windSpeed) km/h", detail: weather.windDescription, systemImage: "wind")
        }
    }

    private func dailyForecastSection(weather: WeatherSnapshot) -> some View {
        WeatherCard {
            VStack(alignment: .leading, spacing: 16) {
                Label("10-DAY FORECAST", systemImage: "calendar")
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(.white.opacity(0.80))

                VStack(spacing: 12) {
                    ForEach(weather.dailyForecasts) { forecast in
                        DailyForecastRow(
                            forecast: forecast,
                            globalLow: weather.globalLow,
                            globalHigh: weather.globalHigh
                        )

                        if forecast.id != weather.dailyForecasts.last?.id {
                            Divider()
                                .overlay(.white.opacity(0.12))
                        }
                    }
                }
            }
        }
    }
}

@MainActor
@Observable
final class WeatherViewModel {
    var cityName = "Current Location"
    var weather: WeatherSnapshot?
    var isLoading = false
    var errorMessage: String?

    private var hasLoaded = false
    private let locationService = LocationService()
    private let weatherService = WeatherService()
    private let geocoder = CLGeocoder()

    var statusText: String {
        if isLoading {
            return "Fetching local weather"
        }

        return "Location access needed"
    }

    var theme: WeatherTheme {
        weather?.theme ?? .dayClear
    }

    func loadWeatherIfNeeded() async {
        guard !hasLoaded else { return }
        hasLoaded = true
        await refreshWeather()
    }

    func refreshWeather() async {
        guard !isLoading else { return }

        isLoading = true
        errorMessage = nil

        do {
            let location = try await locationService.requestCurrentLocation()

            async let weatherTask = weatherService.fetchWeather(
                latitude: location.coordinate.latitude,
                longitude: location.coordinate.longitude
            )
            async let cityTask = resolveCityName(for: location)

            let (snapshot, resolvedCityName) = try await (weatherTask, cityTask)
            weather = snapshot
            cityName = resolvedCityName
        } catch {
            if weather == nil {
                cityName = "Location Unavailable"
            }

            errorMessage = (error as? LocalizedError)?.errorDescription ?? "Unable to fetch the latest weather."
        }

        isLoading = false
    }

    private func resolveCityName(for location: CLLocation) async -> String {
        do {
            let placemarks = try await geocoder.reverseGeocodeLocation(location)
            let placemark = placemarks.first

            if let locality = placemark?.locality, !locality.isEmpty {
                return locality
            }

            if let subAdministrativeArea = placemark?.subAdministrativeArea, !subAdministrativeArea.isEmpty {
                return subAdministrativeArea
            }
        } catch {
            return "Current Location"
        }

        return "Current Location"
    }
}

@MainActor
final class LocationService: NSObject, CLLocationManagerDelegate {
    private let manager = CLLocationManager()
    private var authorizationContinuation: CheckedContinuation<CLAuthorizationStatus, Never>?
    private var locationContinuation: CheckedContinuation<CLLocation, Error>?

    override init() {
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyKilometer
    }

    func requestCurrentLocation() async throws -> CLLocation {
        guard CLLocationManager.locationServicesEnabled() else {
            throw WeatherLoadingError.locationServicesDisabled
        }

        switch manager.authorizationStatus {
        case .authorizedAlways, .authorizedWhenInUse:
            return try await requestSingleLocation()
        case .notDetermined:
            manager.requestWhenInUseAuthorization()
            let status = await waitForAuthorization()

            guard status == .authorizedWhenInUse || status == .authorizedAlways else {
                throw WeatherLoadingError.locationDenied
            }

            return try await requestSingleLocation()
        case .denied, .restricted:
            throw WeatherLoadingError.locationDenied
        @unknown default:
            throw WeatherLoadingError.locationUnavailable
        }
    }

    private func waitForAuthorization() async -> CLAuthorizationStatus {
        await withCheckedContinuation { continuation in
            authorizationContinuation = continuation
        }
    }

    private func requestSingleLocation() async throws -> CLLocation {
        try await withCheckedThrowingContinuation { continuation in
            locationContinuation = continuation
            manager.requestLocation()
        }
    }

    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        authorizationContinuation?.resume(returning: manager.authorizationStatus)
        authorizationContinuation = nil
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let location = locations.first else {
            locationContinuation?.resume(throwing: WeatherLoadingError.locationUnavailable)
            locationContinuation = nil
            return
        }

        locationContinuation?.resume(returning: location)
        locationContinuation = nil
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        locationContinuation?.resume(throwing: error)
        locationContinuation = nil
    }
}

struct WeatherService {
    func fetchWeather(latitude: Double, longitude: Double) async throws -> WeatherSnapshot {
        var components = URLComponents(string: "https://api.open-meteo.com/v1/forecast")
        components?.queryItems = [
            URLQueryItem(name: "latitude", value: String(latitude)),
            URLQueryItem(name: "longitude", value: String(longitude)),
            URLQueryItem(
                name: "current",
                value: [
                    "temperature_2m",
                    "relative_humidity_2m",
                    "apparent_temperature",
                    "is_day",
                    "precipitation",
                    "weather_code",
                    "wind_speed_10m"
                ].joined(separator: ",")
            ),
            URLQueryItem(
                name: "hourly",
                value: [
                    "temperature_2m",
                    "weather_code",
                    "precipitation_probability",
                    "is_day"
                ].joined(separator: ",")
            ),
            URLQueryItem(
                name: "daily",
                value: [
                    "weather_code",
                    "temperature_2m_max",
                    "temperature_2m_min",
                    "sunrise",
                    "sunset",
                    "uv_index_max",
                    "precipitation_probability_max"
                ].joined(separator: ",")
            ),
            URLQueryItem(name: "forecast_days", value: "10"),
            URLQueryItem(name: "timezone", value: "auto"),
            URLQueryItem(name: "wind_speed_unit", value: "kmh")
        ]

        guard let url = components?.url else {
            throw WeatherLoadingError.invalidRequest
        }

        let (data, response) = try await URLSession.shared.data(from: url)

        guard let httpResponse = response as? HTTPURLResponse, 200..<300 ~= httpResponse.statusCode else {
            throw WeatherLoadingError.serverError
        }

        let decodedResponse = try JSONDecoder().decode(OpenMeteoResponse.self, from: data)
        return WeatherSnapshot(response: decodedResponse)
    }
}

private struct WeatherCard<Content: View>: View {
    @ViewBuilder let content: Content

    var body: some View {
        content
            .padding(18)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: 28, style: .continuous)
                    .fill(.ultraThinMaterial.opacity(0.72))
                    .overlay(
                        RoundedRectangle(cornerRadius: 28, style: .continuous)
                            .stroke(.white.opacity(0.12), lineWidth: 1)
                    )
            )
    }
}

private struct WeatherMetricCard: View {
    let title: String
    let value: String
    let detail: String
    let systemImage: String

    var body: some View {
        WeatherCard {
            VStack(alignment: .leading, spacing: 10) {
                Label(title, systemImage: systemImage)
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.white.opacity(0.74))

                Text(value)
                    .font(.system(size: 30, weight: .medium, design: .rounded))
                    .foregroundStyle(.white)

                Text(detail)
                    .font(.subheadline)
                    .foregroundStyle(.white.opacity(0.78))
            }
            .frame(maxWidth: .infinity, minHeight: 132, alignment: .leading)
        }
    }
}

private struct DailyForecastRow: View {
    let forecast: DailyForecast
    let globalLow: Int
    let globalHigh: Int

    var body: some View {
        HStack(spacing: 12) {
            Text(forecast.dayLabel)
                .font(.headline.weight(.medium))
                .foregroundStyle(.white)
                .frame(width: 52, alignment: .leading)

            Image(systemName: forecast.symbolName)
                .font(.body)
                .symbolRenderingMode(.multicolor)
                .frame(width: 24)

            if forecast.precipitationProbability > 0 {
                Text("\(forecast.precipitationProbability)%")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Color(red: 0.50, green: 0.86, blue: 1.0))
                    .frame(width: 40, alignment: .leading)
            } else {
                Text("")
                    .frame(width: 40)
            }

            Text("\(forecast.low)°")
                .font(.subheadline.weight(.medium))
                .foregroundStyle(.white.opacity(0.70))

            GeometryReader { geometry in
                let totalRange = max(globalHigh - globalLow, 1)
                let offsetRatio = CGFloat(forecast.low - globalLow) / CGFloat(totalRange)
                let widthRatio = CGFloat(forecast.high - forecast.low) / CGFloat(totalRange)

                ZStack(alignment: .leading) {
                    Capsule()
                        .fill(.white.opacity(0.16))
                        .frame(height: 4)

                    Capsule()
                        .fill(
                            LinearGradient(
                                colors: [.mint, .yellow, .orange],
                                startPoint: .leading,
                                endPoint: .trailing
                            )
                        )
                        .frame(width: max(geometry.size.width * widthRatio, 24), height: 4)
                        .offset(x: geometry.size.width * offsetRatio)
                }
                .frame(height: 4)
            }
            .frame(height: 4)

            Text("\(forecast.high)°")
                .font(.subheadline.weight(.medium))
                .foregroundStyle(.white)
        }
        .frame(height: 24)
    }
}

private struct WeatherSnapshot {
    let current: CurrentWeather
    let hourlyForecasts: [HourlyForecast]
    let dailyForecasts: [DailyForecast]
    let headline: String
    let theme: WeatherTheme

    init(response: OpenMeteoResponse) {
        let currentCondition = WeatherCondition(
            code: response.current.weatherCode,
            isDaylight: response.current.isDay == 1
        )
        current = CurrentWeather(
            temperature: response.current.temperature2M.roundedInt,
            apparentTemperature: response.current.apparentTemperature.roundedInt,
            humidity: response.current.relativeHumidity2M.roundedInt,
            windSpeed: response.current.windSpeed10M.roundedInt,
            description: currentCondition.description,
            isDaylight: response.current.isDay == 1
        )

        let currentHourIndex = response.hourly.time.firstIndex(of: response.current.time) ?? 0
        let lastHourIndex = min(currentHourIndex + 8, response.hourly.time.count)

        hourlyForecasts = Array(currentHourIndex..<lastHourIndex).map { index in
            let condition = WeatherCondition(
                code: response.hourly.weatherCode[index],
                isDaylight: response.hourly.isDay[index] == 1
            )

            return HourlyForecast(
                timeLabel: index == currentHourIndex ? "Now" : response.hourly.time[index].hourLabel,
                symbolName: condition.symbolName,
                temperature: response.hourly.temperature2M[index].roundedInt,
                precipitationProbability: response.hourly.precipitationProbability[index]
            )
        }

        dailyForecasts = response.daily.time.indices.map { index in
            let condition = WeatherCondition(code: response.daily.weatherCode[index], isDaylight: true)

            return DailyForecast(
                dayLabel: index == 0 ? "Today" : response.daily.time[index].dayLabel,
                symbolName: condition.symbolName,
                low: response.daily.temperature2MMin[index].roundedInt,
                high: response.daily.temperature2MMax[index].roundedInt,
                precipitationProbability: response.daily.precipitationProbabilityMax[index]
            )
        }

        if let rainyHour = hourlyForecasts.first(where: { $0.precipitationProbability >= 45 }) {
            headline = "Rain chance rises to \(rainyHour.precipitationProbability)% by \(rainyHour.timeLabel)."
        } else {
            headline = "Feels like \(current.apparentTemperature)°. \(current.description) through the next few hours."
        }

        theme = WeatherTheme(condition: currentCondition)
    }

    var today: DailyForecast {
        dailyForecasts.first ?? .placeholder
    }

    var globalLow: Int {
        dailyForecasts.map(\.low).min() ?? today.low
    }

    var globalHigh: Int {
        dailyForecasts.map(\.high).max() ?? today.high
    }

    var humidityDescription: String {
        switch current.humidity {
        case ..<40: "Dry air"
        case 40..<70: "Comfortable"
        default: "Humid conditions"
        }
    }

    var uvIndexText: String {
        String(today.uvIndexDisplay)
    }

    var uvDescription: String {
        switch today.uvIndexDisplay {
        case ..<3: "Low exposure"
        case 3..<6: "Moderate"
        case 6..<8: "High"
        case 8..<11: "Very high"
        default: "Extreme"
        }
    }

    var sunPrimary: String {
        today.sunsetLabel
    }

    var sunSecondary: String {
        "Sunrise: \(today.sunriseLabel)"
    }

    var windDescription: String {
        switch current.windSpeed {
        case ..<8: "Light breeze"
        case 8..<20: "Gentle wind"
        case 20..<35: "Breezy"
        default: "Strong wind"
        }
    }
}

private struct CurrentWeather {
    let temperature: Int
    let apparentTemperature: Int
    let humidity: Int
    let windSpeed: Int
    let description: String
    let isDaylight: Bool
}

private struct HourlyForecast: Identifiable {
    let id = UUID()
    let timeLabel: String
    let symbolName: String
    let temperature: Int
    let precipitationProbability: Int
}

private struct DailyForecast: Identifiable {
    let id = UUID()
    let dayLabel: String
    let symbolName: String
    let low: Int
    let high: Int
    let precipitationProbability: Int
    let sunriseLabel: String
    let sunsetLabel: String
    let uvIndexDisplay: Int

    init(
        dayLabel: String,
        symbolName: String,
        low: Int,
        high: Int,
        precipitationProbability: Int,
        sunriseLabel: String = "--:--",
        sunsetLabel: String = "--:--",
        uvIndexDisplay: Int = 0
    ) {
        self.dayLabel = dayLabel
        self.symbolName = symbolName
        self.low = low
        self.high = high
        self.precipitationProbability = precipitationProbability
        self.sunriseLabel = sunriseLabel
        self.sunsetLabel = sunsetLabel
        self.uvIndexDisplay = uvIndexDisplay
    }

    static let placeholder = DailyForecast(dayLabel: "Today", symbolName: "sun.max.fill", low: 0, high: 0, precipitationProbability: 0)
}

private struct WeatherTheme {
    let gradientColors: [Color]

    static let dayClear = WeatherTheme(
        gradientColors: [
            Color(red: 0.09, green: 0.25, blue: 0.52),
            Color(red: 0.24, green: 0.47, blue: 0.83),
            Color(red: 0.67, green: 0.81, blue: 0.95)
        ]
    )

    static let cloudy = WeatherTheme(
        gradientColors: [
            Color(red: 0.22, green: 0.29, blue: 0.38),
            Color(red: 0.38, green: 0.50, blue: 0.64),
            Color(red: 0.63, green: 0.72, blue: 0.80)
        ]
    )

    static let rainy = WeatherTheme(
        gradientColors: [
            Color(red: 0.12, green: 0.18, blue: 0.29),
            Color(red: 0.19, green: 0.31, blue: 0.48),
            Color(red: 0.36, green: 0.50, blue: 0.66)
        ]
    )

    static let night = WeatherTheme(
        gradientColors: [
            Color(red: 0.03, green: 0.06, blue: 0.15),
            Color(red: 0.08, green: 0.13, blue: 0.26),
            Color(red: 0.18, green: 0.26, blue: 0.42)
        ]
    )

    init(condition: WeatherCondition) {
        switch condition.kind {
        case .clear:
            self = condition.isDaylight ? .dayClear : .night
        case .clouds, .fog:
            self = condition.isDaylight ? .cloudy : .night
        case .rain, .snow, .thunder:
            self = .rainy
        }
    }
}

private struct WeatherCondition {
    enum Kind {
        case clear
        case clouds
        case fog
        case rain
        case snow
        case thunder
    }

    let symbolName: String
    let description: String
    let kind: Kind
    let isDaylight: Bool

    init(code: Int, isDaylight: Bool) {
        self.isDaylight = isDaylight

        switch code {
        case 0:
            kind = .clear
            symbolName = isDaylight ? "sun.max.fill" : "moon.stars.fill"
            description = isDaylight ? "Clear Sky" : "Clear Night"
        case 1:
            kind = .clear
            symbolName = isDaylight ? "sun.max.fill" : "moon.stars.fill"
            description = isDaylight ? "Mostly Sunny" : "Mostly Clear"
        case 2:
            kind = .clouds
            symbolName = isDaylight ? "cloud.sun.fill" : "cloud.moon.fill"
            description = "Partly Cloudy"
        case 3:
            kind = .clouds
            symbolName = "cloud.fill"
            description = "Cloudy"
        case 45, 48:
            kind = .fog
            symbolName = "cloud.fog.fill"
            description = "Foggy"
        case 51, 53, 55, 56, 57:
            kind = .rain
            symbolName = "cloud.drizzle.fill"
            description = "Drizzle"
        case 61, 63, 65, 66, 67, 80, 81, 82:
            kind = .rain
            symbolName = "cloud.rain.fill"
            description = "Rain"
        case 71, 73, 75, 77, 85, 86:
            kind = .snow
            symbolName = "snowflake"
            description = "Snow"
        case 95, 96, 99:
            kind = .thunder
            symbolName = "cloud.bolt.rain.fill"
            description = "Thunderstorm"
        default:
            kind = .clouds
            symbolName = "cloud.fill"
            description = "Variable Clouds"
        }
    }
}

private enum WeatherLoadingError: LocalizedError {
    case invalidRequest
    case locationDenied
    case locationServicesDisabled
    case locationUnavailable
    case serverError

    var errorDescription: String? {
        switch self {
        case .invalidRequest:
            return "The weather request could not be created."
        case .locationDenied:
            return "Allow location access in Settings to load weather for your area."
        case .locationServicesDisabled:
            return "Location Services are turned off on this device."
        case .locationUnavailable:
            return "Your location could not be determined right now."
        case .serverError:
            return "The weather service did not respond successfully."
        }
    }
}

private struct OpenMeteoResponse: Decodable {
    let current: Current
    let hourly: Hourly
    let daily: Daily

    struct Current: Decodable {
        let time: String
        let temperature2M: Double
        let relativeHumidity2M: Double
        let apparentTemperature: Double
        let isDay: Int
        let precipitation: Double
        let weatherCode: Int
        let windSpeed10M: Double

        enum CodingKeys: String, CodingKey {
            case time
            case temperature2M = "temperature_2m"
            case relativeHumidity2M = "relative_humidity_2m"
            case apparentTemperature = "apparent_temperature"
            case isDay = "is_day"
            case precipitation
            case weatherCode = "weather_code"
            case windSpeed10M = "wind_speed_10m"
        }
    }

    struct Hourly: Decodable {
        let time: [String]
        let temperature2M: [Double]
        let weatherCode: [Int]
        let precipitationProbability: [Int]
        let isDay: [Int]

        enum CodingKeys: String, CodingKey {
            case time
            case temperature2M = "temperature_2m"
            case weatherCode = "weather_code"
            case precipitationProbability = "precipitation_probability"
            case isDay = "is_day"
        }
    }

    struct Daily: Decodable {
        let time: [String]
        let weatherCode: [Int]
        let temperature2MMax: [Double]
        let temperature2MMin: [Double]
        let sunrise: [String]
        let sunset: [String]
        let uvIndexMax: [Double]
        let precipitationProbabilityMax: [Int]

        enum CodingKeys: String, CodingKey {
            case time
            case weatherCode = "weather_code"
            case temperature2MMax = "temperature_2m_max"
            case temperature2MMin = "temperature_2m_min"
            case sunrise
            case sunset
            case uvIndexMax = "uv_index_max"
            case precipitationProbabilityMax = "precipitation_probability_max"
        }
    }
}

private extension Double {
    var roundedInt: Int {
        Int(self.rounded())
    }
}

private extension String {
    var hourLabel: String {
        DateFormatters.hourOutput.string(from: DateFormatters.hourInput.date(from: self) ?? .now)
    }

    var dayLabel: String {
        DateFormatters.dayOutput.string(from: DateFormatters.dayInput.date(from: self) ?? .now)
    }

    var shortTimeLabel: String {
        DateFormatters.shortTimeOutput.string(from: DateFormatters.hourInput.date(from: self) ?? .now)
    }
}

private enum DateFormatters {
    static let hourInput: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd'T'HH:mm"
        formatter.locale = Locale(identifier: "en_US_POSIX")
        return formatter
    }()

    static let dayInput: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.locale = Locale(identifier: "en_US_POSIX")
        return formatter
    }()

    static let hourOutput: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm"
        return formatter
    }()

    static let shortTimeOutput: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm"
        return formatter
    }()

    static let dayOutput: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "EEE"
        return formatter
    }()
}

#Preview {
    ContentView()
}
