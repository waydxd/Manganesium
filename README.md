
<div align="center" style="display: flex; align-items: center; gap: 10px;">
    
<h2 style="margin: 0;"> <img src="web/src/assets/gradient.png" alt="logo" width="700" /></h2>
</div>
<h3 align="center">
 Powering Your Knowledge with Elemental Speed! 🚀
</h3>


<p align="center">
  <a href="https://github.com/waydxd/COMP4321-Group25/blob/main/LICENSE"><img src="https://img.shields.io/github/license/waydxd/COMP4321-Group25?color=blue" alt="License"></a>
 <img src="https://img.shields.io/badge/Status-In%20Progress-yellow" alt="Project Status">
    <img src="https://img.shields.io/github/last-commit/waydxd/COMP4321-Group25?label=Last%20Commit" alt="Last Commit">
  <img src="https://img.shields.io/github/repo-size/waydxd/COMP4321-Group25?label=Repo%20Size" alt="Repository Size">
  <a href="https://github.com/waydxd/COMP4321-Group25"><img src="https://img.shields.io/github/stars/waydxd/COMP4321-Group25?style=social" alt="GitHub Stars"></a>
</p>


## 📖 Project Description
This project is a comprehensive search engine application developed as part of the COMP4321 course by Group 25. The application leverages multiple technologies and frameworks to deliver a robust and efficient solution.
Design documents and other project-related files can be found in the [docs](docs) directory.
## 📷 Screenshots of UI
![frontpage.png](docs/Screenshots/Frontpage.png)
![search.png](docs/Screenshots/search.png)
## 📋 Table of Contents
- [Technologies Used](#technologies-used)
- [Installation](#installation)
- [Usage](#usage)
- [Contributing](#contributing)
- [License](#license)
```
Manganesium
├── 📁 app
│   ├── Purpose: Hosts the Ktor server and invokes functions in the search module for rank calculations and other operations (Final Phase); also runs the full pipeline (crawler, indexer, and test program for Phase 1)
│   ├── Usage: Executes main workflow, generates `spider_result.txt` for Phase 1, and serves as the backend server for the Final Phase
│   └── Dependencies: crawler, indexer, search, utils, web
├── 📁 crawler
│   ├── Purpose: Crawls web pages and stores metadata/links in the database
│   ├── Usage: Fetches 30 pages (Phase 1) or 300 pages (Final), populates `db.pages`, `urlToPageId`, `parentToChildLinks`, `childToParentLinks`
│   └── Dependencies: utils, indexer
├── 📁 indexer
│   ├── Purpose: Indexes pages from the database for search functionality
│   ├── Usage: Builds inverted, forward indexes (`bodyInvertedIndex`, `titleInvertedIndex`) and `documentFrequency` from `db.pages`
│   └── Dependencies: utils
├── 📁 search
│   ├── Purpose: Implements the search engine backend for ranked results (Final Phase)
│   ├── Usage: Queries inverted indexes for ranked results, handles rank calculations and other search-related operations
│   └── Dependencies: utils (for db access)
├── 📁 utils
│   ├── Purpose: Provides shared utilities for all modules
│   ├── Usage: Reusable code for database access, and other common tasks
│   └── Dependencies: None (standalone)
└── 📁 web
    ├── Purpose: Front-end interface for search engine (Final Phase)
    ├── Usage: Displays search results via a web UI
    └── Dependencies: only communicate with app modules (standalone)
    
```
## 🛠 Technologies Used
The project is built using the following technologies:
- **Vue.js & Vite** (frontend)
- **Kotlin** (backend)
  - MapDB
  - Jsoup
  - Gradle
  - JUnit
  - SLF4J
  - Kotlin-Logging
  - Logback
  - Mockito
  - Ktor

## 🔧 Installation
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

## 🚀 Usage
To run the *project test program* for **Phase 1**, follow these steps:
```bash
./gradlew runTest
``` 
or if you have gradle installed:
```bash
gradle runTest
```
then the `crawler.db`, `indexer.db` and `spider_test.txt` will be generated in the root directory.

Alternatively, you can run the test program by running the `Main.kt` file in the `app/src/main/kotlin` directory using IntelliJ IDEA.

To run the **search engine backend** for **Final Phase**, follow these steps:
```bash
./gradlew run
```
or if you have gradle installed:
```bash
gradle run
```
Then the search engine will start crawling first and then the api listener will start listening on port 8080. You can access the api by navigating to `http://localhost:8080/api/health` in your web browser.

You can specify port by setting the `PORT` environment variable. For example, to run the backend on port 8081, you can run:
```bash
PORT=8081 gradle run
```
_Note that if you changed to use ports other than 8080. You will need to specify the backend address in web/.env in order to let the frontend to bind it. 
An example .env.default has been provided for you for reference._

For the frontend vue app, navigate to the `web` directory and run the following command:
```bash
npm run build
npm run preview
```
or if you have vite installed globally:
```bash
vite build
vite preview
```
Then the frontend website will be available at `http://localhost:4173` in your web browser.

For simplicity, a run.sh has been made to run the backend and frontend at the same time. To run the script, navigate to the root directory and run:
```bash
chmod +x run.sh
./run.sh
```
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
This project is licensed under the GPL-3.0 license. See the [LICENSE](LICENSE) file for details.
