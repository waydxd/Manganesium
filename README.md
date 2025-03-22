# COMP4321 Group 25 - Manganesium

## Project Description
This project is a comprehensive application developed as part of the COMP4321 course by Group 25. The application leverages multiple technologies and frameworks to deliver a robust and efficient solution.
Design documents and other project-related files can be found in the [docs](docs) directory.
## Table of Contents
- [Technologies Used](#technologies-used)
- [Installation](#installation)
- [Usage](#usage)
- [Contributing](#contributing)
- [License](#license)
```
Manganesium
├── app
│   ├── Purpose: Integrates and runs the full pipeline (crawler, indexer, and test program for Phase 1; search and web later)
│   ├── Usage: Executes main workflow, generates spider_result.txt for Phase 1
│   └── Dependencies: crawler, indexer, utils (later: search, web)
├── crawler
│   ├── Purpose: Crawls web pages and stores metadata/links in the database
│   ├── Usage: Fetches 30 pages (Phase 1) or 300 pages (Final), populates db.pages, urlToPageId, parentToChildLinks, childToParentLinks
│   └─── Dependencies: utils, indexer
├── indexer
│   ├── Purpose: Indexes pages from the database for search functionality
│   ├── Usage: Builds inverted, forward indexes (bodyInvertedIndex, titleInvertedIndex) and documentFrequency from db.pages
│   └── Dependencies: utils
├── search
│   ├── Purpose: Implements the search engine backend (not in Phase 1)
│   ├── Usage: Queries inverted indexes for ranked results (Final Submission)
│   └── Dependencies: utils (later: indexer for db access)
├── utils
│   ├── Purpose: Provides shared utilities for all modules
│   ├── Usage: Reusable code for database access, and other common tasks
│   └── Dependencies: None (standalone)
└─── web
    ├── Purpose: Front-end interface for search engine (not in Phase 1)
    ├── Usage: Displays search results via a web UI (Final Submission)
    ├── Dependencies: search, utils
    └── Files: (TBD for Final Submission)
```
## Technologies Used
The project is built using the following technologies:
- **Vue.js**
- **Kotlin**
  - MapDB
  - Jsoup
  - Gradle
  - JUnit
  - SLF4J
  - Kotlin-Logging
  - Logback
  - Mockito

## Installation
To install and run this project locally, follow these steps:

1. **(Optional if you have the repo zip) Clone the repository**:
    ```bash
    git clone https://github.com/waydxd/COMP4321-Group25.git
    ```

2. **Navigate to the project directory**:
    ```bash
    cd COMP4321-Group25
    ```

3. **Install dependencies**:
    - For the Vue.js frontend (Not implemented in phase 1):
      ```bash
      cd web
      npm install
      ```
    - For the Kotlin backend:
      ```bash
      ./gradlew build
      ```
      or if you have gradle installed:
      ```bash
      gradle build
      ```

## Usage
To run the project test program, follow these steps:
```bash
./gradlew run
``` 
or if you have gradle installed:
```bash
gradle run
```
then the `crawler.db`, `indexer.db` and `spider_test.txt` will be generated in the root directory.

Alternatively, you can run the test program by running the `Main.kt` file in the `app/src/main/kotlin` directory using IntelliJ IDEA.
## Contributing
Contributions are welcome! Please follow these steps to contribute:

1. **Fork the repository**.
2. **Create a new branch**:
    ```bash
    git checkout -b feature/your-feature-name
    ```
3. **Make your changes and commit them**:
    ```bash
    git commit -m 'Add some feature'
    ```
4. **Push to the branch**:
    ```bash
    git push origin feature/your-feature-name
    ```
5. **Create a new Pull Request**.

## License
This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
