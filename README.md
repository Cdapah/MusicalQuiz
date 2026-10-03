# MusicalQuiz - Final Project Report


## 1. App Functionalities

MusicalQuiz is an Android application designed to provide a music exploration and quiz experience. The application leverages the Deezer API and integrates multiple local features. The main functionalities include:


### Search Functionality
- Users can search for music tracks or albums using the search interface
- Search results are displayed in a visually appealing grid layout
- Each result shows the album cover, track/album title
- Users can tap on a result to view detailed information


### Details View
- Detailed information about tracks or albums
- For tracks: displays title, artist, album, and duration
- For albums: displays title, release date, and track listing
- Preview playback functionality for tracks
- Option to add tracks to custom playlists


### Playlist Management
- Create, view, and manage custom playlists
- Add tracks to playlists from search results or details view
- Delete or rename playlists as needed


### Quiz Functionality
- Create quizzes based on playlists
- Multiple quiz modes:
  - Multiple choice: select the correct track title from options
  - Open-ended: type the complete track title
- Configurable time limits for questions
- Score tracking and results display

## 2. Technical Architecture

The MusicalQuiz app is built using the MVVM (Model-View-ViewModel) architecture pattern, which provides a clean separation of concerns and makes the codebase more maintainable, testable, and scalable.

### Architecture Diagram

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│      Views      │     │   ViewModels    │     │     Models      │
│  (Fragments)    │◄────┤                 │◄────┤                 │
│                 │     │                 │     │                 │
└────────┬────────┘     └────────┬────────┘     └────────┬────────┘
         │                       │                       │
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│    UI Layer     │     │  Business Logic │     │    Data Layer   │
│                 │     │                 │     │                 │
│ - Fragments     │     │ - ViewModels    │     │ - Repositories  │
│ - Adapters      │     │ - LiveData      │     │ - SQLite DB     │
│ - Layouts       │     │                 │     │ - Deezer API    │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

### Key Components

1. **UI Layer**
   - Single Activity with multiple Fragments for different screens
   - Bottom Navigation for screen navigation
   - RecyclerViews with custom adapters for displaying lists
   - Material Design components for a modern UI

2. **Business Logic Layer**
   - ViewModels for each screen to manage UI-related data
   - LiveData for observable data patterns
   - Coroutines for asynchronous operations

3. **Data Layer**
   - Repository pattern to abstract data sources
   - SQLite database for local storage (playlists, tracks, quizzes)
   - Retrofit for API communication with Deezer
   - Glide for image loading

### Database Schema

The app uses SQLite for local storage with the following tables:

- **playlists**: Stores user-created playlists
- **tracks**: Stores track information from Deezer
- **playlist_tracks**: Junction table linking playlists and tracks
- **quizzes**: Stores quiz information
- **quiz_questions**: Stores questions for each quiz
- **quiz_answers**: Stores possible answers for multiple-choice questions

### Implementation Details

- **Navigation Component**: Used for managing fragment navigation
- **ViewModels and LiveData**: Used for reactive UI updates
- **Retrofit**: Used for API communication
- **SQLite**: Used for local database storage
- **MediaPlayer**: Used for audio preview playback
- **Glide**: Used for image loading
- **Coroutines**: Used for asynchronous operations

## 3. Technical issues and Future Improvements

While the current implementation of MusicalQuiz provides a solid foundation, there are several technical issues and potential improvements that could be addressed in future versions:

### Current Technical issues

   - The app currently requires an internet connection for most functionality
   - Track previews may occasionally fail due to API/network limitations.
   - Quiz UI responsiveness can be improved during audio playback.
   - Offline support is partial; only IDs are stored, not full metadata.
   - Test coverage for database operations and API responses is incomplete.


### Future Improvements

   - View all tracks in a playlist
   - Add a track from album to pkaylist directly
   - Migrate to Room database for better type safety and query validation
   - Add comprehensive unit and UI tests
   
## 4. Contributions

We worked collaboratively on many aspects of the project, especially while testing, debugging, and integrating modules in person.

### Carole
- Developed the playlist and quiz modules:
  - PlaylistFragment, PlaylistViewModel, PlaylistRepository
  - QuizFragment, QuizPlayFragment, QuizViewModel, QuizRepository

### Fatima
- Implemented the search functionality:
- SearchFragment, SearchViewModel, SearchResultAdapter
- Designed and added most of the drawable resources

### Shared Work
- Database implementation and configuration
- General UI integration and navigation setup
- Joint testing and code reviews



