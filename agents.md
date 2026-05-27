# ResearchCenter Architecture & Structure Reference

This document provides an overview of the system architecture based on the System Design Document (SDD) and the implemented vertical slice architecture in the codebase, alongside a reference for all frontend layouts and file structures.

## 1. Backend Architecture (Vertical Slice)

The backend (Spring Boot 17+) follows a **Vertical Slice Architecture**, organizing code by features/domains rather than technical layers (e.g., separating all controllers from services). 

Each feature slice encapsulates its own controllers, services, repositories, models, and DTOs:

### Implemented Vertical Slices (`backend/researchcenter/src/main/java/edu/cit/uy/researchcenter/features`)
* **`activity`**: Tracks user actions within the system (`ActivityController`, `ActivityService`, `Activity`, `ActivityRepository`).
* **`admin`**: Handles system administration and moderation (`AdminController`).
* **`auth`**: Manages registration, login, JWT issuance, and Google OAuth (`AuthController`, `AuthService`, `GoogleAuthService`, `JwtService`).
* **`bookMetadata`**: Integrates with Google Books API for auto-fetching book details (`GoogleBooksService`).
* **`material`**: Manages academic materials, PDFs (via Supabase), bookmarks, and tags (`MaterialController`, `MaterialService`, `SupabaseStorageService`).
* **`repository`**: Handles creation and management of research repositories, invitations, and membership (`RepositoryController`, `RepositoryService`, `RepositoryExtraService`).
* **`request`**: Manages the material request and fulfillment system within repositories (`RequestController`, `RequestService`).
* **`user`**: Handles user profile operations (`UserController`, `UserService`).

### Shared Core (`backend/researchcenter/src/main/java/edu/cit/uy/researchcenter/shared`)
Cross-cutting concerns are kept in a shared module:
* **Config**: `JwtAuthFilter`, `SecurityConfig`, `WebClientConfig`
* **Exception**: `GlobalExceptionHandler`
* **Response**: Standardized `ApiResponse` wrappers
* **Services**: Global services like `EmailService` (SendGrid SMTP)

---

## 2. Frontend Layouts & Structure Reference

The frontend (React 18 + Vite + TypeScript + Tailwind CSS) mirrors the backend's feature-based approach, keeping UI components, pages, and API calls grouped by domain.

### Layouts
While the main application layout is orchestrated in `App.tsx` using standard wrappers (`Navbar.tsx`, `ProtectedRoute.tsx`), the system includes specific layout files for complex nested views:
* **`AdminLayout.tsx`** (`web/src/features/admin/AdminLayout.tsx`): The wrapper layout for all admin dashboard pages, including sidebars and admin-specific navigation.

### Feature Pages & Components Reference (`web/src/features`)

* **`activity/`**
  * `ActivitiesPage.tsx`
  * `ActivityFeed.tsx`
  * `NotificationsPage.tsx`
* **`admin/`**
  * `AdminLayout.tsx` (Layout)
  * `AdminActivityPage.tsx`
  * `AdminMaterialsPage.tsx`
  * `AdminRepositoriesPage.tsx`
  * `AdminRequestsPage.tsx`
  * `AdminStatsPage.tsx`
  * `AdminUsersPage.tsx`
* **`auth/`**
  * `LoginPage.tsx`
  * `RegisterPage.tsx`
* **`bookmarks/`**
  * `BookmarksPage.tsx`
* **`dashboard/`**
  * `DashboardPage.tsx`
  * Components: `RepositoryCard.tsx`
* **`invite/`**
  * `AcceptInvitePage.tsx`
* **`material/`**
  * `AddMaterialPage.tsx`
  * `EditMaterialPage.tsx`
  * Components: `MaterialForm.tsx`
* **`profile/`**
  * `ProfilePage.tsx`
* **`repository/`**
  * `RepositoryDetailPage.tsx`
* **`request/`**
  * `NewRequestPage.tsx`

### Shared Components (`web/src/shared/components`)
Reusable UI elements utilized across different feature slices:
* `AppLogo.tsx`
* `Breadcrumbs.tsx`
* `ConfirmModal.tsx`
* `LoadingScreen.tsx`
* `Navbar.tsx`
* `NotificationDropdown.tsx`
* `ProtectedRoute.tsx` / `PublicRoute.tsx`
* `ResearchLoader.tsx`
* `UserAvatar.tsx`

---

## 3. Technology Stack

Based on the SDD, the system employs a modern, three-tier architecture:

* **Backend**: Java 17, Spring Boot 3.x, Spring Security, Spring Data JPA, Spring WebSocket, JJWT
* **Database**: PostgreSQL 14+ (hosted on Supabase)
* **Storage**: Supabase Storage (for PDF files)
* **Email Service**: SendGrid (SMTP)
* **Web Frontend**: React 18, Vite, TypeScript, Tailwind CSS, Axios
* **Mobile App**: Kotlin, XML Layouts, Retrofit, Room Database
* **Build Tools**: Maven (Backend), npm (Web), Gradle (Android)
* **Deployment Targets**: Render (Backend), Vercel (Web)

---

## 4. Current State of Mobile Application

The mobile application (Android) is initialized and well-developed, mirroring the feature-based Vertical Slice Architecture of the backend. 

Key implementations currently present in the codebase (`mobile/ResearchCenter/app/src/main/`):
* **Architecture Setup**: Developed natively using **Kotlin** and **XML Layouts** (following the SDD requirement to use XML over Jetpack Compose).
* **Feature Activities & Fragments**: Separate packages and views are fully implemented for `auth`, `admin`, `dashboard`, `invite`, `material`, `notifications`, `profile`, `repository`, and `request`.
* **Networking Layer**: Integrates `Retrofit` (`RetrofitClient`, API interfaces) for REST communication and a `NotificationWebSocketClient` for real-time WebSocket events.
* **Local Data & Caching**: Utilizes the **Room Database** (`AppDatabase`, `MaterialDao`, `RepositoryDao`) for offline/local storage capabilities.
* **UI/UX Components**: Comprehensive XML layouts (`activity_*.xml`, `fragment_*.xml`, `item_*.xml`, `dialog_*.xml`) have been created, reflecting a fully fleshed-out mobile user interface complete with bottom navigation, badges, avatars, bottom sheets, and custom views.
