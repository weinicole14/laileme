import SwiftUI

struct ContentView: View {
    @EnvironmentObject var authManager: AuthManager
    @EnvironmentObject var themeManager: ThemeManager
    @State private var selectedTab: Tab = .home

    enum Tab: String, CaseIterable {
        case home = "首页"
        case calendar = "日历"
        case discover = "发现"
        case profile = "我的"

        var icon: String {
            switch self {
            case .home: return "house.fill"
            case .calendar: return "calendar"
            case .discover: return "sparkles"
            case .profile: return "person.fill"
            }
        }
    }

    var body: some View {
        VStack(spacing: 0) {
            // 主内容区
            Group {
                switch selectedTab {
                case .home:
                    HomeScreen()
                case .calendar:
                    CalendarScreen()
                case .discover:
                    DiscoverScreen()
                case .profile:
                    ProfileScreen()
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)

            // 底部导航栏
            BottomNavBar(selectedTab: $selectedTab)
        }
        .ignoresSafeArea(.keyboard)
    }
}
