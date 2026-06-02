re# Prompt: Android Kotlin XML — ResearchCenter Native App (User-Side Only)

You are building a **pixel-faithful Android native replica** of the ResearchCenter web app using **Kotlin + XML layouts** (View system, not Compose). The goal is a carbon copy of the web UI translated faithfully into Material Design 3 Android conventions, matching every color, spacing, card style, icon, typography weight, and interaction pattern as closely as possible.

---

## 🎨 Design System / Brand Tokens

| Token | Value |
|---|---|
| **Primary / Brand Green** | `#16a34a` (Tailwind `green-600`) |
| **Primary Dark** | `#15803d` (hover, `green-700`) |
| **Primary Light** | `#dcfce7` (bg tints, `green-100`) |
| **Primary Text on Green** | `#14532d` (`green-900`) |
| **Background** | `#f9fafb` (`gray-50`) |
| **Surface (cards)** | `#ffffff` |
| **Border** | `#e5e7eb` (`gray-200`) |
| **Border Focus** | `#16a34a` |
| **Text Primary** | `#111827` (`gray-900`) |
| **Text Secondary** | `#6b7280` (`gray-600`) |
| **Text Tertiary** | `#9ca3af` (`gray-400`) |
| **Error** | `#dc2626` (`red-600`) |
| **Error Bg** | `#fef2f2` (`red-50`) |
| **Warning/Pending** | `#d97706` (`amber-600`) |
| **Info/Open badge** | `#1d4ed8` (`blue-700`) |
| **Font** | `Inter` (load via Google Fonts or use `sans-serif` system fallback) |
| **Corner Radius** | Cards: `8dp`; Modals: `16dp`; Chips/badges: `999dp` (pill); Buttons: `6dp` |
| **Navbar height** | `56dp` |
| **Card elevation** | `2dp shadow` with `1dp border #e5e7eb` |

---

## 📐 App Architecture

### Navigation
- **Bottom Navigation Bar** with 4 tabs (icon + label):
  1. `Dashboard` — home/grid icon
  2. `Bookmarks` — bookmark icon  
  3. `Notifications` — bell icon (with unread badge dot, green `#16a34a`)
  4. `Profile` — person icon
- **Top App Bar** on every authenticated screen:
  - Left: ResearchCenter logo (small square logo mark + "ResearchCenter" wordmark in green)
  - Right: notification bell icon button (badge dot if unread) + user avatar circle (initials or photo, `40dp`, `green-700` bg)
  - Height: `56dp`, white bg, `1dp` bottom border `#e5e7eb`, subtle shadow
- **No Drawer** — the web app uses no sidebar

### Screen Stack
```
SplashScreen → LoginActivity
                    └─> RegisterActivity (link)
LoginActivity → MainActivity (hosts bottom nav + fragments)
    ├─ DashboardFragment
    ├─ BookmarksFragment
    ├─ NotificationsFragment
    └─ ProfileFragment

DashboardFragment → RepositoryDetailActivity (full screen)
    └─ RepositoryDetailActivity tabs:
         ├─ Materials tab → MaterialDetailBottomSheet
         │                → AddMaterialActivity / EditMaterialActivity
         ├─ Bookmarks tab (within repo)
         ├─ Requests tab → RequestDetailBottomSheet
         ├─ Members tab
         ├─ Updates tab
         └─ Activity tab

DashboardFragment → CreateRepositoryBottomSheet (modal)
ProfileFragment → EditProfileBottomSheet
```

---

## 🔐 Screen 1: LoginActivity

**Layout:** `activity_login.xml`

- **Full screen**, background `#f9fafb`
- **Header bar** at top: white, `1dp` bottom border, centered logo (logo.png 64dp + text.svg 40dp tall), height `~80dp`
- **Card** centered vertically, `white` bg, `8dp` corners, `2dp` shadow, `1dp` border `#e5e7eb`, horizontal margin `16dp`, padding `32dp`
  - `H1`: "Sign In" — `24sp`, bold, `#111827`
  - Subtitle: "Enter your credentials to access your account" — `14sp`, `#6b7280`, `4dp` top margin
  - **Email field** (top margin `24dp`):
    - Label: "Email Address" — `14sp`, medium weight, `#111827`, `4dp` bottom margin
    - `TextInputLayout` outlined style, hint `you@example.com`, `rounded 6dp`, focus color `#16a34a`
  - **Password field** (`16dp` top margin):
    - Label: "Password"
    - `TextInputLayout` with `endIconMode="password_toggle"` (eye icon), placeholder `••••••••`
    - Focus ring: `#16a34a`
  - **Error text** (visible only on error): `14sp`, `#dc2626`, bold, `role=alert`
  - **Sign In button**: full width, `44dp` min height, `bg #16a34a`, white text, `14sp` semibold, `6dp` corners, `16dp` top margin. Disabled state: `60%` opacity
  - **Divider** with "or" text centered (`16dp` margins top/bottom)
  - **"Sign in with Google" button**: outlined, white bg, Google logo left, text "Sign in with Google", `44dp` height, full width, `6dp` corners, border `#e5e7eb`
  - **Footer link**: `14sp` centered — "Don't have an account? **Sign Up**" — "Sign Up" in `#16a34a` bold

**Behavior:**
- `POST /api/v1/auth/login` with `{ email, password }`
- On success: save `accessToken`, `refreshToken`, user object to `EncryptedSharedPreferences`
- On `401`: show "Invalid email or password"
- On suspended (`403` or error code `AUTH-003`): show "Your account is suspended. Please contact administrator"
- Google Sign-In: use Google Identity Services SDK → `POST /api/v1/auth/google` with `{ idToken }`
- Clear error when user edits either field

---

## 📝 Screen 2: RegisterActivity

**Layout:** `activity_register.xml`

Same header bar as Login. Card titled "Create Account" with subtitle "Join ResearchCenter to collaborate on research".

Form fields (all `TextInputLayout` outlined, focus `#16a34a`):
1. **First Name + Last Name** — side-by-side in `LinearLayout` horizontal, weight `1:1`, `8dp` gap
2. **Email Address**
3. **Password** (with eye toggle) — hint text below: "At least 8 characters" `12sp gray-600`
4. **Confirm Password** (with eye toggle)

Inline field errors (`12sp`, `#dc2626`, appear below each field).

**Create Account** button (full width, green, same as login).

"or" divider → **Sign up with Google** button.

Footer: "Already have an account? **Sign In**"

**Validation:**
- All fields required
- Password ≥ 8 chars
- Passwords must match
- `POST /api/v1/auth/register` → on success navigate to Login (passing email) OR directly in if coming from invite link

---

## 🏠 Screen 3: DashboardFragment

**Layout:** `fragment_dashboard.xml`

Background: `#f9fafb`

**Content (ScrollView):**

```
[Welcome section]  ← 16dp all padding
  "Welcome back, {firstname}!"  ← 24sp bold gray-900
  "Manage your research repositories and collaborate with your team"  ← 14sp gray-600

[+ Create New Research Repository button]
  ← full width BUT left-aligned, NOT full width
  ← Actually: wrap_content button, green bg #16a34a, white text 14sp semibold
  ← "+ Create New Research Repository", 6dp corners, horizontal padding 16dp, vertical 10dp
  ← 24dp top margin, 16dp bottom margin

[Error banner if any]  ← red-50 bg, red-200 border, red-700 text, 8dp corners, 12dp padding

[Two-column layout: repos (2/3 width) + activity feed sidebar (1/3)]
  On mobile → stack vertically: repos section first, then ActivityFeed below
```

**Your Research Repositories section:**
- Section header: `18sp` semibold `gray-900`, `"Your Research Repositories (N)"`
- Grid of **RepositoryCards** — 1 column on mobile

**RepositoryCard** (`item_repository_card.xml`):
- White bg, `8dp` corners, `2dp` shadow, `1dp` border `#e5e7eb`
- Padding: `16dp`
- **Top row**: repo name (`16sp` semibold `gray-900`, truncate 1 line) + **star/bookmark icon** (filled `#16a34a` if favorited, outline `gray-400` if not) — right-aligned
- **Description**: `13sp` `gray-600`, 2 lines max, `4dp` top margin
- **Stats row** (`8dp` top margin): people icon + memberCount + book icon + materialCount — `12sp` `gray-500`
- **Last activity**: `12sp` `gray-400`, `4dp` top margin
- **Action row** (only for owned repos): `Edit` (pencil icon button) + `Delete` (trash icon button) — small, gray, right-aligned at bottom
- Tap the card → open RepositoryDetailActivity
- Long-press or tap star → toggle favorite (optimistic update)

**Invited Repositories section** (same card style, but no Edit/Delete buttons, just tap to open)

**Empty state**: centered card with "No repositories yet" + subtitle

**ActivityFeed sidebar** (on mobile: full width card below repos):
- White card, `8dp` corners, border `#e5e7eb`
- Header: "Recent Activity" + Activity icon — `16sp` semibold
- List of activity items (icon + description + time) — `13sp`
- Scrollable, max 10 items visible
- Action icons per activity type: `BookOpen`=blue, `FolderPlus`=green, `UserPlus`=purple, `CheckCircle`=green, `XCircle`=red, `Trash2`=red, `LogOut`=gray
- Each item: icon (in colored circle 32dp) + text description + relative time right-aligned

**Create Repository BottomSheet** (`CreateRepositoryBottomSheet`):
- White bg, `16dp` top corners, drag handle
- Title: "Create New Research Repository" `18sp` bold
- Name field: `TextInputLayout` outlined, label "Name", hint "e.g., ML Research"
- Description field: `TextInputLayout` outlined, multiline 4 lines, label "Description"
- Buttons row: `Cancel` (outlined, gray) + `Create` (filled green) — weight 1:1

**Edit Repository BottomSheet** (same structure, pre-populated, title "Edit Repository")

**Delete Confirm Dialog:**
- `MaterialAlertDialog` — title "Delete Repository", message "Delete {name}? This removes the repository for everyone.", buttons: Cancel (text) + Delete (red tint)

**API calls:**
- `GET /api/v1/repositories` → load all
- `POST /api/v1/repositories` → create
- `PUT /api/v1/repositories/{id}` → edit
- `DELETE /api/v1/repositories/{id}` → delete
- `POST /api/v1/repositories/{id}/bookmark` → toggle favorite

---

## 📚 Screen 4: RepositoryDetailActivity

**Layout:** `activity_repository_detail.xml`

Full screen with custom toolbar (back arrow + repo name truncated + overflow menu).

**Toolbar actions (overflow menu):**
- Owner: "Edit Repository", "Delete Repository", "Leave" (not applicable)
- Member: "Leave Repository"

**Breadcrumb** below toolbar: "Dashboard > {Repo Name}" in `12sp` gray-500

**Repo header card** (below breadcrumbs):
- Repo name `20sp` bold
- Description (collapsible if >2 lines, "Show more" link in green)
- Stats: N members • N materials • N requests

**6-tab TabLayout** (scrollable if needed):
1. `Materials`
2. `Bookmarks`
3. `Requests`
4. `Members`
5. `Updates`
6. `Activity`

Tab indicator color: `#16a34a`. Selected tab text: `#16a34a`. Unselected: `#6b7280`.

---

### Tab 1: Materials

**Toolbar row** (between tabs and list):
- `Search` field (magnifier icon, rounded, `gray-100` bg) — searches title
- `Tags` dropdown chip — opens tag filter sheet (green border when active)
- `Filters` dropdown chip — opens filter sheet (Status, Type, Uploader)
- Sort toggle: Latest / Oldest (small chip)

**Material list** (RecyclerView):

Each `item_material.xml`:
- White card, `8dp` corners, border `#e5e7eb`, padding `16dp`
- **Top row**: material title `15sp` semibold `gray-900` (truncate 2 lines) + bookmark icon (filled green if bookmarked)
- **Type badge**: pill chip — PDF=`blue-100/blue-700`, LINK=`purple-100/purple-700`, REFERENCE=`green-100/green-700`, `12sp`, semibold
- **Uploaded by + date**: `12sp` `gray-500`
- **Status badge**: pill chip — TO_READ=`gray-100/gray-600`, IN_PROGRESS=`amber-100/amber-700`, COMPLETED=`green-100/green-700`
- **Tags**: horizontal chip row, `11sp`, `gray-100` bg, `gray-700` text
- Tap → open **Material Detail BottomSheet**
- Owner/uploader: shows Edit (pencil) + Delete (trash) icon buttons, top-right

**Material Detail BottomSheet** (full height expandable):
- Drag handle, rounded top corners `16dp`
- Title `20sp` bold, type badge, status badge
- Uploaded by + date — `13sp` gray
- Description (if any)
- For PDF: **"Open PDF"** button (green, opens in browser/PDF viewer) + copy URL button
- For LINK: **"Open Link"** button (green, opens in Chrome)
- For REFERENCE: ISBN (with copy button), publisher, year, authors — formatted metadata card
- Bookmark toggle button (icon + "Bookmark" / "Bookmarked" text, green)
- **Status selector**: horizontal chip row or spinner — TO_READ / IN_PROGRESS / COMPLETED (calls `PATCH /api/v1/materials/{id}/status`)
- **My Note** section:
  - Label: "My Note" `14sp` semibold
  - Multiline text field, placeholder "Add a private note about this material..."
  - Save Note button (text button, green, right-aligned)
- For owners: Edit + Delete buttons at bottom

**Add/Edit Material Activity** (`AddMaterialActivity`, `EditMaterialActivity`):
- Toolbar: back + "Add Material" / "Edit Material" title
- **Type selector**: 3 toggle chips — PDF / Link / Reference — green selected
- Fields change by type:
  - **PDF**: title, description, tags, upload PDF button (shows filename), `≤10MB`, PDF only
  - **LINK**: title, description, URL field, tags
  - **REFERENCE**: ISBN search bar + "Search Google Books" button (fetches metadata autofill), title, description, publisher, year, authors (comma-separated), tags
- Tags field: comma-separated input with hint "e.g. machine learning, NLP"
- Submit button: green, full width, "Save Material"

**API:**
- `GET /api/v1/repositories/{id}/materials`
- `POST /api/v1/materials` (JSON) or `POST /api/v1/materials/upload?repositoryId={id}` (multipart)
- `PUT /api/v1/materials/{id}`
- `DELETE /api/v1/materials/{id}`
- `PATCH /api/v1/materials/{id}/status`
- `POST /api/v1/materials/{id}/bookmark`
- `GET /api/v1/materials/{id}/note` / `PUT /api/v1/materials/{id}/note`

---

### Tab 2: Bookmarks (within Repo)

Same as Materials tab but filtered to `bookmarked = true`. Shows bookmark icon. Paginated (10 per page).

---

### Tab 3: Requests

**Header row:** Search field + Filters chip (status, requester) + Sort (latest/oldest)

Each `item_request.xml`:
- White card, `8dp` corners, border
- Title `15sp` semibold + status badge (OPEN=`blue-100/blue-700`, FULFILLED=`green-100/green-700`, PENDING=`amber-100/amber-700`, CANCELLED=`gray-100/gray-600`)
- Requested by + date `12sp` gray
- Tap → **Request Detail BottomSheet**

**Request Detail BottomSheet:**
- Title `20sp` bold
- "Requested by {name} • {date}" `13sp` gray
- Status badge + fulfilled by info (if fulfilled)
- Description card (`gray-50` bg)
- **If FULFILLED**: "Attached Material" section — tappable green card showing material title
- **If OPEN + user is NOT requester**: "Fulfill Request" section:
  - Spinner/dropdown: select existing material OR "+ Attach a new material..." (navigates to AddMaterial)
  - "Fulfill Request" green button
- **Edit material** option if already fulfilled (for non-requester)
- Owner: Delete request button

**New Request Activity** (`NewRequestActivity`):
- Title field + Description field
- Submit green button: `POST /api/v1/repositories/{id}/requests`

**API:**
- `GET /api/v1/repositories/{id}/requests`
- `POST /api/v1/repositories/{id}/requests`
- `POST /api/v1/requests/{id}/fulfill` → `{ materialId }`
- `PUT /api/v1/requests/{id}/material` → `{ materialId }`
- `DELETE /api/v1/requests/{id}`

---

### Tab 4: Members

**Invite Member section** (owner only):
- Email search field with autocomplete dropdown (debounce 400ms, `GET /api/v1/users/search?email={email}`)
- Result card: shows user avatar + full name + email → tap "Invite" button → `POST /api/v1/repositories/{id}/invite` `{ email }`
- If not found: "No user found with this email"

**Members list** (RecyclerView):
- Each item: user avatar (32dp circle, initials if no photo) + full name + "Owner" / "Member" badge
- Owner badge: `green-100/green-700` pill
- Owner can remove members: trash icon, confirms via dialog
- Paginated (10 per page)

**Leave Repository** (members only): shown as button at bottom of tab or in overflow menu

**API:**
- `GET /api/v1/repositories/{id}/members`
- `POST /api/v1/repositories/{id}/invite`
- `DELETE /api/v1/repositories/{id}/members/{userId}`
- `POST /api/v1/repositories/{id}/leave`

---

### Tab 5: Updates

**Post update** (text field + Post button, owner only):
- Multiline `TextInputLayout`, placeholder "Share an update with your team..."
- "Post" button (green, right-aligned below)

**Updates list** (sorted newest-first):
- Each item: avatar + name + relative time + content text
- Author can edit (pencil icon, turns field editable inline) or delete (trash, confirm dialog)

**Pagination** (10 per page, Previous/Next buttons)

**API:**
- `GET /api/v1/repositories/{id}/updates`
- `POST /api/v1/repositories/{id}/updates`
- `PUT /api/v1/repositories/{id}/updates/{updateId}`
- `DELETE /api/v1/repositories/{id}/updates/{updateId}`

---

### Tab 6: Activity

Full `ActivityFeed` component for this repository (see ActivityFeed description in Dashboard). Shows all activity events scoped to the repo, newest first, paginated.

---

## 🔖 Screen 5: BookmarksFragment

**Layout:** `fragment_bookmarks.xml`

- Page title row: Bookmark icon (green, `24dp`) + "My Bookmarks" `22sp` bold + subtitle `14sp` gray
- Search field: full-width, `gray-300` border, `#16a34a` focus, magnifier icon left, `Search bookmarks...` hint
- **White card** containing:
  - Grid 1 column (mobile) of bookmark cards
  - Each `item_bookmark.xml`:
    - `gray-50` bg, `8dp` corners, `gray-200` border, hover → `green-300` border
    - Title `14sp` semibold, bookmark icon (filled green, top-right)
    - Type + "From Repository" — `12sp` gray-500
    - Description 2 lines max `12sp` gray-600
    - Tap → navigate to `RepositoryDetailActivity` for that repo
  - Empty state: large Bookmark icon (light gray) + "No bookmarks found" + helper text
  - Pagination: Previous / Page N of M / Next

**API:** `GET /api/v1/materials/bookmarked`

---

## 🔔 Screen 6: NotificationsFragment

**Layout:** `fragment_notifications.xml`

- Header row: Bell icon in `emerald-50` circle + "Notifications" `22sp` bold, bottom border `emerald-100`
- Activity list (load 10 at a time, "Load more" button at bottom)
- Each `item_notification.xml`:
  - White card, `8dp` corners, `emerald-100` border
  - Icon (action-specific, colored circle `32dp`) + message text + relative time right-aligned
  - Tap → navigate to relevant repo + correct tab
- Empty state: large Bell icon (light green) + "No notifications yet"
- Real-time updates via WebSocket (mark seen on page open via `localStorage`-equivalent SharedPreferences key `rc_seen_activity_id`)

**API:** `GET /api/v1/activities/notifications?page={n}&size=10`

---

## 👤 Screen 7: ProfileFragment

**Layout:** `fragment_profile.xml`

Background `#f9fafb`.

**Back button** (top-left text button "← Back") — navigates back.

**Page header**: "Your Profile" `22sp` bold + subtitle `14sp` gray.

**3-column grid → on mobile: vertical stack:**

**Column 1 — Profile Card** (white, `8dp` corners, shadow, border):
- **Avatar** `96dp` circle: if `profilePicture` URL → load image; else initials in `green-700` bg
- When editing: Camera overlay button (green circle, bottom-right of avatar) → opens image picker → upload to Supabase → `PUT /api/v1/users/me`
- "Remove photo" button (red text, only if has photo + editing mode)
- Name `18sp` semibold (or edit fields for First/Last Name when in edit mode)
- Email `14sp` gray-600 (read-only, shield icon prefix)
- Role badge: "USER" — `green-100/green-700` pill
- **Edit Profile** / **Save** / **Cancel** buttons (pencil icon → edit mode; check + X when editing)
- **Log Out** button (red text with LogOut icon) at bottom of card

**Column 2+3 — Stats + Activity:**

**Account Stats card** (white, border):
- "Account Statistics" `16sp` semibold
- 3 stat rows: 📅 Member since {date} | 📁 Repositories created: N | 🗓️ Days active: N

**Recent Activity card** (full ActivityFeed for the current user's repos, last 10 items):
- Same ActivityFeed component, `fullPage=false`

**API:**
- `GET /api/v1/users/me`
- `PUT /api/v1/users/me` → `{ firstname, lastname, profilePicture? }`
- Supabase upload for avatar (use same Supabase storage URL pattern)

---

## 🔗 Screen 8: AcceptInviteActivity

**Layout:** `activity_accept_invite.xml`

- Centered card with logo, title "You've Been Invited", repository name, accept/decline buttons
- `GET /api/v1/invitations/accept?token={token}` on accept

---

## ⚙️ Technical Implementation Notes

### Authentication
- Store tokens in `EncryptedSharedPreferences`
- Attach `Authorization: Bearer {accessToken}` to every API request via `OkHttp Interceptor`
- On `401` response: attempt token refresh via `POST /api/v1/auth/refresh` with `refreshToken`
- On refresh failure: clear session, navigate to LoginActivity
- Base URL: configurable via `BuildConfig.BASE_URL` (default `http://10.0.2.2:8080/api/v1` for emulator)

### Networking
- Use **Retrofit 2** + **OkHttp** + **Gson** converter
- API response wrapper: `{ data: T, message: String, success: Boolean }`
- All endpoints under `/api/v1/`

### WebSocket (Real-time)
- Connect to `ws://{host}/ws` with `Authorization` header after login
- Subscribe to repository-scoped topics
- Reconnect on disconnect with exponential backoff
- On any message: refresh relevant fragment/tab data

### Image Loading
- Use **Coil** or **Glide** for avatar + cover images
- Circle crop for avatars

### Loading States
- Use `CircularProgressIndicator` (green tint `#16a34a`) centered on screen
- Skeleton shimmer effect optional for cards

### Error States
- Red banner `#fef2f2` bg, `#dc2626` text, `8dp` corners, full-width, dismissible
- Toast for transient feedback (green bg for success, red for error) — bottom of screen, `16dp` margins

### Suspended Account Modal
- `MaterialAlertDialog` blocking (not dismissable): "Account Suspended" title, message, "Go to Login" red button → clears session → LoginActivity

### Pagination
- Previous / Next text buttons with chevron icons
- "Page N of M" centered between them
- `14sp`, `gray-500`, disabled state `50%` opacity

### Status / Type Badges (reusable `BadgeView`)
Pill-shaped `TextView`, `999dp` corner radius:
- TO_READ: bg `#f3f4f6`, text `#374151`
- IN_PROGRESS: bg `#fef3c7`, text `#b45309`
- COMPLETED: bg `#dcfce7`, text `#15803d`
- OPEN: bg `#dbeafe`, text `#1d4ed8`
- FULFILLED: bg `#dcfce7`, text `#166534`
- PDF: bg `#dbeafe`, text `#1d4ed8`
- LINK: bg `#f3e8ff`, text `#7e22ce`
- REFERENCE: bg `#dcfce7`, text `#15803d`

### Confirm Dialogs (ConfirmModal equivalent)
Use `MaterialAlertDialog`:
- Title + message
- Cancel (text button, gray)
- Confirm (text button — red tint for danger actions like Delete, green for normal)

### UserAvatar Component (reusable `UserAvatarView`)
- Circle, `32dp` or `40dp` depending on context
- If `profilePicture` URL → Coil/Glide with circle crop
- Else → initials (first char of firstname + first char of lastname), white text, `green-700` bg

---

## 📱 Key UX Details to Match Web

1. **Favorite/Star repos**: star icon top-right of card, tap toggles — optimistic update (instant visual, revert on API error)
2. **Bookmark materials**: bookmark icon on material card + in detail sheet — same optimistic pattern
3. **Tags filter**: multi-select chip list, tag filter button shows count badge if active
4. **Filters dropdown**: bottom sheet with checkbox groups for Status, Type, Uploader
5. **Sort toggle**: "Latest" / "Oldest" — chips or toggle group
6. **Material note**: saved per user per material, private (not visible to others)
7. **Activity feed icons**: each action type has a specific colored icon (see ActivityFeed above)
8. **Repository owner vs member**: owner sees Edit + Delete on repo cards; member sees only Open
9. **Invite autocomplete**: debounced email search shows found user card with avatar + name + Invite button
10. **Request fulfill flow**: select from existing materials OR "+ Attach new material" (navigates to add material then returns to fulfill)
11. **ISBN copy**: copy button next to ISBN in material detail, "Copied!" toast for 2 seconds
12. **Repo description expand/collapse**: "Show more" / "Show less" link if description > 2 lines

---

## 🚫 Excluded (Admin UI — Do Not Build)

Do NOT build any of these admin screens:
- AdminStats, AdminUsers, AdminRepositories, AdminMaterials, AdminRequests, AdminAnnouncements, AdminActivity
- Admin layout / sidebar navigation

The app is **user-side only**. If a logged-in user has `role == ADMIN`, redirect them to Dashboard (or show a "Please use the web admin panel" message).

---

## 📦 Suggested Dependencies (build.gradle)

```kotlin
// Networking
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

// Image Loading
implementation("io.coil-kt:coil:2.6.0")

// Security
implementation("androidx.security:security-crypto:1.1.0-alpha06")

// Navigation
implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

// UI
implementation("com.google.android.material:material:1.12.0")
implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.0")

// Google Sign-In
implementation("com.google.android.gms:play-services-auth:21.2.0")

// WebSocket
implementation("com.squareup.okhttp3:okhttp:4.12.0")
```

---

## ✅ Deliverables Expected

For each screen/component, generate:
1. **XML layout file** (`res/layout/`) — match spacing, colors, typography exactly
2. **Kotlin Activity/Fragment/ViewModel** — full logic, API calls, state management
3. **Retrofit service interface** — all endpoints
4. **Data models** — matching TypeScript types exactly
5. **RecyclerView adapters** for all lists
6. **BottomSheetDialogFragment** for all modal flows
7. **Reusable components**: `UserAvatarView`, `BadgeView`, `ConfirmDialog`, `ActivityFeedAdapter`

Build the **user-side app in full** — every screen, every interaction, every API call — as a faithful 1:1 Android native replica of the ResearchCenter web app.
