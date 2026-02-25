# SAR Project - HTTP/1.1 Server with SSE Support

The project goal is to implement a Java-based HTTP/1.1 server that conects to a database (containing information on groups of students). To obtain grades higher than 17 the student must also implement Server-Sent Events (SSE) for real-time update of database operations in all subscribed client browsers. 

This initial project serves as an educational starting boilerplate that serves as a base to start coding the project and as an initial structure for understanding web server architecture, design patterns, and event-driven programming.

## Table of Contents
- [Project Overview](#project-overview)
- [Architecture](#architecture)
- [Design Patterns](#design-patterns)
- [Component Reference](#component-reference)
- [Student Implementation Guide](#student-implementation-guide)
- [Running the Project](#running-the-project)
- [Testing](#testing)

---

## Project Overview

This project implements a raw socket-based HTTP/1.0/1.1 server with the following features:

### Core Features (Required Implementation)
- **HTTP Request/Response Handling**: Parse incoming HTTP requests and generate proper responses
- **Static File Serving**: Serve HTML, CSS, JavaScript, and image files
- **RESTful API**: Info on Student Groups management endpoints (create, read, update, delete)
- **Cookie Support**: Session management using HTTP cookies
- **MongoDB Integration**: Persistent storage for group data
- **SSL/TLS Support**: Secure HTTPS connections on a separate port with re-direction from regular HTTP. 

### Bonus Features (Optional - Scores above 17)
- **Server-Sent Events (SSE)**: Real-time broadcast of group updates to connected clients
- **Event Broadcasting**: Push notifications when groups are created, updated, or deleted

### Technical Stack
- **Java 21**: Modern Java with enhanced features
- **MongoDB**: NoSQL database for group storage
- **Foundation CSS**: Responsive frontend framework
- **SLF4J + Logback**: Structured logging

---

## Architecture

### Request Flow Diagram

```
Client Browser
    ↓
    ↓ HTTP Request
    ↓
[ServerThread] ← Accepts connections on ports 20000 (HTTP) and 20043 (HTTPS)
    ↓
    ↓ Spawns thread per connection
    ↓
[ConnectionThread] ← Parses raw HTTP request bytes (ASCII text in HTTP1.0/1.1)
    ↓
    ↓ Creates Request object
    ↓
[HttpController] ← Re directs to HTTPS if request received in HTTP and routes request to appropriate handler
    ↓
    ↓ Matches endpoint (static files, /api, /events, etc.)
    ↓
[Handler] ← AbstractRequestHandler implementation
    ├─ ApiHandler (/api)
    ├─ EventHandler (/events) [SSE]
    └─ StaticFileHandler (default)
    ↓
    ↓ Calls business logic
    ↓
[Service Layer] ← GroupServiceImpl
    ↓
    ↓ Database operations
    ↓
[Repository Layer] ← MongoGroupRepository
    ↓
    ↓ MongoDB queries
    ↓
[MongoDB Database]

Response flows back through the same layers
```

### Layer Responsibilities

| Layer | Components | Responsibilities |
|-------|-----------|------------------|
| **Server** | `Main`, `ServerThread`, `ConnectionThread` | Accept connections, parse HTTP, manage threads |
| **Controller** | `HttpController` | Route requests to appropriate handlers |
| **Handler** | `ApiHandler`, `EventHandler`, `StaticFileHandler` | Handle specific endpoints, generate responses |
| **Service** | `GroupServiceImpl`, `EventBroadcaster` | Business logic, validation, event coordination |
| **Repository** | `MongoGroupRepository` | Database operations, data persistence |
| **Model** | `Group`, `Request`, `Response`, `Headers` | Data structures |

---

## Design Patterns
The project is structured according to the best practices in software design patterns.
### 1. Template Method Pattern
**Location**: `AbstractRequestHandler`

The `handle()` method defines the skeleton of request processing:
```java
public void handle(Request request, Response response) {
    preHandle(request, response);   // Hook method
    
    if (GET request)
        handleGet(request, response);    // Abstract - must override
    else if (POST request)
        handlePost(request, response);   // Abstract - must override
    
    postHandle(request, response);  // Hook method
}
```

**How to extend**: Create a handler class extending `AbstractRequestHandler` and implement `handleGet()` and `handlePost()` methods.

**Example**:
```java
public class EventHandler extends AbstractRequestHandler {
    @Override
    protected void handleGet(Request request, Response response) {
        // Set SSE headers
        // Register client with EventBroadcaster
        // Keep connection open
    }
    
    @Override
    protected void handlePost(Request request, Response response) {
        // Return error (POST not supported for SSE)
    }
}
```

### 2. Repository Pattern
**Location**: `GroupRepository` interface, `MongoGroupRepository` implementation

Abstracts data access logic from business logic. The service layer (`GroupServiceImpl`) depends on the `GroupRepository` interface, not the concrete MongoDB implementation.

**Benefits**:
- Easy to switch databases (e.g., MySQL, PostgreSQL)
- Testable with mock repositories
- Separation of concerns

**Where to extend**: Implement new repository methods in both the interface and `MongoGroupRepository`.

### 3. Service Pattern
**Location**: `GroupService` interface, `GroupServiceImpl` implementation

Encapsulates business logic separate from data access and presentation layers.

**Responsibilities**:
- Input validation
- Business rule enforcement
- Orchestrating repository calls
- Event broadcasting coordination

**Where to extend**: Add new business operations by updating the interface and implementation.

### 4. Dependency Injection
**Location**: `Main.java`

All components are initialized in a specific order with dependencies injected via constructors:

```java
// 1. Database
MongoClient mongoClient = initializeMongoClient();

// 2. Repository (depends on mongoClient)
GroupRepository repository = new MongoGroupRepository(mongoClient);

// 3. Service (depends on repository)
GroupService service = new GroupServiceImpl(repository);

// 4. EventBroadcaster (no dependencies)
EventBroadcaster broadcaster = new EventBroadcaster();

// 5. Handlers (depend on services)
ApiHandler apiHandler = new ApiHandler(service);
EventHandler eventHandler = new EventHandler(broadcaster);

// 6. Controller (depends on handlers)
HttpController controller = new HttpController(apiHandler, eventHandler, staticHandler);
```

**Why this matters**: Understanding the initialization order helps you know where to inject new components.

### 5. Observer/Broadcaster Pattern (SSE)
**Location**: `EventBroadcaster`

Maintains a list of connected SSE clients and broadcasts events to all of them when groups change.

**Flow**:
1. Client connects to `/events` endpoint → `EventHandler` registers client with `EventBroadcaster`
2. Group created/updated/deleted → `GroupServiceImpl` calls `eventBroadcaster.broadcast(eventData)`
3. `EventBroadcaster` pushes event to all registered clients
4. Clients receive real-time updates without polling

---

## Component Reference

### Server Components

#### `Main.java`
**Purpose**: Application entry point and dependency injection container

**Key Methods**:
- `main()`: Initializes SSL context and starts server
- `initializeXXX()`: Component initialization methods following Dependency Injection pattern
- `startServer()`: Creates HTTP (port 20000) and HTTPS (port 20043) server sockets

**Configuration Constants**:
- `HTTPport = 20000`: HTTP server port
- `HTTPSport = 20043`: HTTPS server port
- `GROUP_SIZE = 2`: Number of members per group
- `StaticFiles = "html"`: Directory for static content

#### `ServerThread.java`
**Purpose**: Accepts incoming client connections in a loop

**Behavior**: Runs indefinitely, accepts connections, spawns `ConnectionThread` for each client.

#### `ConnectionThread.java`
**Purpose**: Handles individual HTTP request/response cycle and connection management

**Key Responsibilities**:
- Read raw bytes from socket input stream
- Parse HTTP method, URL, version, headers
- Read POST body if present
- Create `Request` object
- Delegate to `HttpController` for routing
- Send response back to client
- Handle connection reuse (HTTP keep-alive) or close connection

**Where to implement**:
- **Step 0 (HTTP fundamentals)**: Request header parsing, response header generation, HTTP→HTTPS redirection, TCP connection reuse with keep-alive loop
- **POST parameter parsing**: Parse `application/x-www-form-urlencoded` data from request body into `request.postParameters`

### Controller & Routing

#### `HttpController.java`
**Purpose**: Route requests to appropriate handlers based on URL

**Routing Logic**:
```
URL ends with "api" → ApiHandler
URL ends with "events" → EventHandler
Otherwise → StaticFileHandler (default)
```

**How to add new endpoints**: Call `registerHandler("endpoint", handlerInstance)` in the constructor.

### Handlers

#### `AbstractRequestHandler.java`
**Purpose**: Base class for all request handlers using Template Method pattern

**Override Points**:
- `handleGet()`: Handle GET requests (required)
- `handlePost()`: Handle POST requests (required)
- `preHandle()`: Pre-processing hook (optional)
- `postHandle()`: Post-processing hook (optional)

#### `ApiHandler.java`
**Purpose**: RESTful JSON API endpoint for group management

**Current State**: Returns placeholder JSON responses

**Endpoints**:
- GET `/api`: Retrieve all groups as JSON for AJAX clients (e.g., index.html table)
- POST `/api`: Create or update a group from form data, return JSON success/error

**Where to implement**:
- `handleGet()`: Fetch groups from `groupService.getAllGroups()`, format as JSON array, set appropriate headers
- `handlePost()`: Parse parameters from `request.postParameters`, validate input, call `groupService.saveGroup()` or `deleteGroup()`, return JSON response
- Set proper HTTP headers: `Content-Type: application/json`, `Content-Length`

#### `EventHandler.java`
**Purpose**: Handle `/events` endpoint for Server-Sent Events (SSE)

**Current State**: Skeleton with no implementation

**Where to implement**:
- `handleGet()`:
  1. Set SSE headers: `Content-Type: text/event-stream`, `Cache-Control: no-cache`
  2. Get output stream from response
  3. Register output stream with `eventBroadcaster.registerClient(outputStream)`
  4. Keep connection open (don't close stream; may need to loop or wait)
  5. Handle client disconnection and cleanup

**SSE Protocol Reference**:
```
HTTP/1.1 200 OK
Content-Type: text/event-stream
Cache-Control: no-cache
Connection: keep-alive

data: {"type": "group.created", "groupNumber": "42"}\n\n
data: {"type": "group.deleted", "groupNumber": "13"}\n\n
```

#### `StaticFileHandler.java`
**Purpose**: Serve files from the `html/` directory

**Features**:
- MIME type detection (`.html`, `.css`, `.js`, `.jpg`, `.png`, `.gif`)
- 404 responses for missing files
- Default handler when no specific endpoint matches

### Service Layer

#### `GroupServiceImpl.java`
**Purpose**: Business logic for group management

**Key Methods**:
- `saveGroup()`: Create or update a group
- `deleteGroup()`: Remove a group
- `getAllGroups()`: Retrieve all groups
- `incrementAccessCount()`: Track group access
- `generateGroupHtml()`: Render groups as HTML table

**SSE Integration Points** (marked in code comments):
- After `saveGroup()`: Broadcast "group.created" or "group.updated" event
- After `deleteGroup()`: Broadcast "group.deleted" event
- After `incrementAccessCount()`: Optionally broadcast "group.accessed" event

**How to integrate EventBroadcaster**:
1. Add `EventBroadcaster` field to class
2. Update constructor to accept it as parameter
3. Update `Main.initializeGroupService()` to pass broadcaster
4. Call `eventBroadcaster.broadcast(eventData)` after operations

#### `EventBroadcaster.java`
**Purpose**: Manage SSE client connections and broadcast events

**Current State**: Skeleton with method signatures

**Thread Safety Requirement**: Multiple `ConnectionThread` instances may call methods simultaneously. Use thread-safe collections (e.g., `CopyOnWriteArrayList`) or synchronization.

**Where to implement**:
- `registerClient(OutputStream)`: Add client to list of active connections
- `removeClient(OutputStream)`: Remove disconnected client
- `broadcast(String eventData)`: Send event to all clients in SSE format
  - Format: `"data: " + eventData + "\n\n"`
  - Handle I/O exceptions (client disconnected)
  - Remove failed clients from list

**Example Implementation Approach**:
```java
public void broadcast(String eventData) {
    String sseMessage = "data: " + eventData + "\n\n";
    
    // Iterate over clients, send message
    for (OutputStream client : clients) {
        try {
            client.write(sseMessage.getBytes());
            client.flush();
        } catch (IOException e) {
            // Client disconnected, remove from list
            removeClient(client);
        }
    }
}
```

### Repository Layer

#### `MongoGroupRepository.java`
**Purpose**: MongoDB data access implementation

**Database**: `sardb`, Collection: `groups`

**Key Operations**:
- `save()`: Insert or update (upsert) group
- `findByGroupNumber()`: Retrieve specific group
- `findAll()`: Get all groups
- `delete()`: Remove group
- `incrementAccessCount()`: Atomic counter increment

**Data Conversion**: Converts between `Group` objects and MongoDB `Document` objects.

### HTTP Protocol Classes

#### `Request.java`
**Purpose**: Immutable HTTP request data holder

**Key Fields**:
- `method`: HTTP method (GET, POST, etc.)
- `urlText`: Request URL
- `version`: HTTP version (HTTP/1.0 or HTTP/1.1)
- `headers`: HTTP headers as `Headers` object
- `cookies`: Parsed cookies as `Properties`
- `postParameters`: POST form data as `Properties` **(currently unpopulated - students implement)**
- `bodyText`: Raw POST body

#### `Response.java`
**Purpose**: HTTP response builder

**Key Methods**:
- `setCode(ReplyCode)`: Set HTTP status code
- `setHeader(name, value)`: Add response header
- `setText(String)`: Set response body (text/HTML)
- `setFile(File)`: Set response body (file)
- `send_Answer(OutputStream)`: Send response to client

**Common Usage**:
```java
response.setCode(ReplyCode.OK);
response.setHeader("Content-Type", "text/html");
response.setText("<html>...</html>");
```

#### `Headers.java`
**Purpose**: Wrapper around `Properties` for HTTP headers

Provides convenience methods for header parsing and access.

---

## Student Implementation Guide

### Required Core Implementations

#### 0. HTTP Protocol Fundamentals
**Files**: `ConnectionThread.java`, `StaticFileHandler.java`, `ApiHandler.java`

**Goal**: Before implementing higher-level features, understand and implement the basic HTTP/1.1 protocol mechanisms. This step teaches HTTP header handling, redirection, and connection management.

**Part A: Request Header Parsing**  
**File**: `ConnectionThread.java`

Currently the server reads but doesn't fully parse HTTP request headers. Headers contain critical information like cookies, content type, user agent, and connection preferences.

**What to implement**:
- Parse request headers into the `Headers` object
- Handle multi-line header values (if needed)
- Extract specific headers: `Cookie`, `User-Agent`, `Content-Type`, `Connection`
- Store in `request.headers` for use by handlers

**Part B: Response Header Generation**  
**Files**: `StaticFileHandler.java`, `ApiHandler.java`

HTTP responses must include appropriate headers for content type, caching, and connection management.

**What to implement**:
- Set `Content-Type` header based on file extension (StaticFileHandler) or data type (ApiHandler)
  - `.html` → `text/html; charset=UTF-8`
  - `.css` → `text/css`
  - `.js` → `application/javascript`
  - `.json` → `application/json`
- Set `Content-Length` header with response body size
- Set `Connection: keep-alive` or `Connection: close` based on request
- Send headers before response body

**Part C: HTTP to HTTPS Redirection**  
**File**: `ConnectionThread.java`

When the server receives HTTP requests on the non-SSL socket, it should redirect to HTTPS.

**What to implement**:
- Detect if the connection is non-SSL (check socket type or port)
- Return HTTP 301 (Moved Permanently) or 307 (Temporary Redirect)
- Set `Location` header to HTTPS URL: `https://hostname:sslPort/originalPath`
- Example: `Location: https://localhost:9001/api`

**Part D: TCP Connection Reuse (Keep-Alive)**  
**File**: `ConnectionThread.java`

HTTP/1.1 allows multiple requests over a single TCP connection, reducing latency.

**What to implement**:
- After processing one request/response, check if `Connection: keep-alive` was sent
- Loop back to read the next request instead of closing the socket
- Set timeout to prevent indefinite waiting
- Close socket when `Connection: close` is received or timeout occurs

**Learning outcome**: These fundamentals are essential for all web servers. Understanding headers, redirection, and keep-alive prepares you for implementing REST APIs and SSE.

---

#### 1. POST Parameter Parsing
**File**: `ConnectionThread.java`

**Problem**: POST request body is read but not parsed into `request.postParameters`.

**What to implement**:
- Parse `application/x-www-form-urlencoded` data
- Split by `&` to get key-value pairs
- Split each pair by `=` 
- URL-decode values
- Populate `request.postParameters` Properties object

**Location**: After reading the request body (`bodyText`), parse it and populate `postParameters`.

**Example**:
```
Body: "groupNumber=42&number0=12345&name0=John&counter=on"
Should populate:
  postParameters.put("groupNumber", "42")
  postParameters.put("number0", "12345")
  postParameters.put("name0", "John")
  postParameters.put("counter", "on")
```

#### 2. API Handler POST Implementation
**File**: `ApiHandler.java`

**Problem**: `handlePost()` is incomplete.

**What to implement**:
- Extract POST parameters from `request.postParameters`
- Parse group data: `groupNumber`, member arrays (`number0`, `number1`, `name0`, `name1`), `counter` checkbox
- Validate input (non-empty, correct count)
- Call `groupService.saveGroup(groupNumber, numbers, names, counter)`
- Set appropriate response (success message or error)
- Optionally set a cookie with last group number

**Integration with cookies**:
```java
response.setHeader("Set-Cookie", "lastGroupNumber=" + groupNumber + "; Path=/");
```

#### 3. Cookie Parsing
**File**: `ConnectionThread.java`

**Problem**: Cookie header is read but `parseCookies()` isn't called.

**What to implement**:
- Call `request.parseCookies()` during request parsing
- Ensure `Cookie` header is passed to the method
- Format: `"Cookie: name1=value1; name2=value2"`

### Bonus Implementation (SSE for Higher Scores)

#### 4. EventHandler - SSE Connection Management
**File**: `EventHandler.java`

**What to implement in `handleGet()`**:

**Step 1**: Set SSE response headers
```java
response.setHeader("Content-Type", "text/event-stream");
response.setHeader("Cache-Control", "no-cache");
response.setHeader("Connection", "keep-alive");
```

**Step 2**: Get output stream from socket (stored in response or passed separately)
```java
OutputStream outputStream = // ... get from response or connection
```

**Step 3**: Register client with broadcaster
```java
eventBroadcaster.registerClient(outputStream);
```

**Step 4**: Keep connection alive
- Option A: Loop indefinitely until client disconnects
- Option B: Don't close the stream, rely on connection timeout
- Handle exceptions and cleanup on disconnect

**Challenge**: SSE connections must stay open indefinitely, unlike normal HTTP. The thread handling the connection cannot terminate normally.

#### 5. EventBroadcaster - Thread-Safe Broadcasting
**File**: `EventBroadcaster.java`

**What to implement**:

**`registerClient(OutputStream)`**:
- Add client to the `clients` list
- Thread-safe: `clients.add(clientStream)`

**`removeClient(OutputStream)`**:
- Remove client from list
- Thread-safe: `clients.remove(clientStream)`

**`broadcast(String eventData)`**:
- Format message as SSE: `"data: " + eventData + "\n\n"`
- Iterate through all clients
- Write message to each client's OutputStream
- Flush to ensure immediate delivery
- Catch `IOException` for disconnected clients and remove them

**Thread Safety**: Use `CopyOnWriteArrayList` for `clients` or synchronize access.

#### 6. Service Layer - Event Broadcasting Integration
**File**: `GroupServiceImpl.java`

**What to implement**:

**Step 1**: Add `EventBroadcaster` field and inject via constructor
```java
private final EventBroadcaster eventBroadcaster;

public GroupServiceImpl(GroupRepository repository, EventBroadcaster broadcaster) {
    this.repository = repository;
    this.eventBroadcaster = broadcaster;
}
```

**Step 2**: Update `Main.java` to pass broadcaster when creating service:
```java
this.groupService = new GroupServiceImpl(this.groupRepository, this.eventBroadcaster);
```

**Step 3**: Call `broadcast()` after operations:

In `saveGroup()`:
```java
repository.save(group);
String eventType = isNewGroup ? "group.created" : "group.updated";
String eventData = "{\"type\":\"" + eventType + "\",\"groupNumber\":\"" + groupNumber + "\"}";
eventBroadcaster.broadcast(eventData);
```

In `deleteGroup()`:
```java
repository.delete(groupNumber);
String eventData = "{\"type\":\"group.deleted\",\"groupNumber\":\"" + groupNumber + "\"}";
eventBroadcaster.broadcast(eventData);
```

**Event Data Format**: JSON is recommended for easy client-side parsing, but plain text works too.

---

## Running the Project

### Prerequisites
- **Java 21** installed
- **MongoDB** running on `localhost:27017`
- **Maven** for dependency management

### Database Setup
```bash
# Start MongoDB (if not running)
mongod

# The application will create the "sardb" database and "groups" collection automatically
```

### Build and Run
```bash
# Compile
mvn clean compile

# Run
mvn exec:java -Dexec.mainClass="com.sar.server.Main"

# Or run Main.java directly from IDE
```

### Access Points
- **HTTP Server**: http://localhost:20000
- **HTTPS Server**: https://localhost:20043
- **Main Page**: http://localhost:20000/index.htm
- **API Endpoint**: http://localhost:20000/api
- **SSE Endpoint**: http://localhost:20000/events (bonus feature)

---

## Testing

### Test Page Overview
The included `index.htm` provides comprehensive testing for all features:

#### Core Feature Tests
1. **Form Submission (POST)**:
   - Fill out group form
   - Click "Create/Update Group"
   - Verify success message appears
   - Check groups table updates

2. **Cookie Handling**:
   - Create a group
   - Observe "Cookie Test" section updates with last group number
   - Refresh page and verify cookie persists

3. **Group Display (GET)**:
   - Click "Refresh Groups" button
   - Verify all groups from database are displayed in table

4. **Group Deletion (DELETE)**:
   - Enter group number
   - Click "Delete Group"
   - Verify group is removed from table

#### SSE Feature Tests (Bonus)
5. **SSE Connection**:
   - Click "Connect to SSE" button
   - Status should change to "Connected" with green indicator
   - Event log should show connection message

6. **Real-time Updates**:
   - Keep SSE connected
   - Create/delete a group in another browser tab/window
   - Verify event appears in "Server-Sent Events" log
   - Verify groups table updates automatically without refresh

### Manual Verification

**Test POST Parsing**:
```bash
curl -X POST http://localhost:20000/api \
  -d "groupNumber=99&number0=11111&name0=Alice&number1=22222&name1=Bob&counter=on"
```

**Test SSE Connection**:
```bash
curl -N http://localhost:20000/events
# Should keep connection open and receive events
```

**Test Static Files**:
```bash
curl http://localhost:20000/index.htm
# Should return HTML content
```

### Expected Behavior

| Feature | Expected Result |
|---------|----------------|
| Load index.htm | Page displays with form, groups table, and SSE panel |
| Submit group form | Success message, cookie updated, groups table refreshed |
| Click refresh | Groups table reloads from server |
| Connect to SSE (if implemented) | Green indicator, "Connected" status |
| Create group with SSE active | Event appears in log, table updates automatically |

### Debugging Tips

**Problem**: Groups don't save
- Check POST parameter parsing in `ConnectionThread`
- Verify `ApiHandler.handlePost()` implementation
- Check MongoDB connection and database name

**Problem**: SSE disconnects immediately
- Verify SSE headers are set correctly
- Ensure connection stays open (no stream close)
- Check `EventHandler.handleGet()` doesn't return immediately

**Problem**: Events not broadcasting
- Verify `EventBroadcaster` is injected into `GroupServiceImpl`
- Check `broadcast()` method is called after group operations
- Ensure SSE message format is correct: `"data: ...\n\n"`

**Problem**: Multiple events received
- Each `ConnectionThread` might trigger broadcast
- Normal behavior; clients should deduplicate if needed

---

## Project Structure Summary

```
src/main/java/
├── com/sar/
│   ├── controller/
│   │   └── HttpController.java          # Routes requests to handlers
│   ├── model/
│   │   └── Group.java                   # Group data model
│   ├── repository/
│   │   ├── GroupRepository.java         # Repository interface
│   │   └── MongoGroupRepository.java    # MongoDB implementation
│   ├── server/
│   │   ├── Main.java                    # Entry point, DI container
│   │   ├── ServerThread.java            # Accepts connections
│   │   └── ConnectionThread.java        # Handles individual requests
│   ├── web/
│   │   ├── handler/
│   │   │   ├── AbstractRequestHandler.java  # Template method base
│   │   │   ├── ApiHandler.java              # /api endpoint
│   │   │   ├── EventHandler.java            # /events SSE endpoint
│   │   │   └── StaticFileHandler.java       # Static files
│   │   └── http/
│   │       ├── Request.java             # HTTP request object
│   │       ├── Response.java            # HTTP response builder
│   │       ├── Headers.java             # Headers wrapper
│   │       └── ReplyCode.java           # HTTP status codes
│   └── service/
│       ├── EventBroadcaster.java        # SSE event broadcaster
│       ├── GroupService.java            # Service interface
│       └── GroupServiceImpl.java        # Business logic implementation
└── config/
    └── MongoConfig.java                 # MongoDB connection config

html/
├── index.htm                            # Main test page
├── css/                                 # Foundation CSS framework
├── js/                                  # JavaScript libraries
└── img/                                 # Images

pom.xml                                  # Maven dependencies
keystore                                 # SSL certificate keystore
```

---

## Learning Objectives

By implementing this project, students will understand:

1. **HTTP Protocol**: Request/response format, headers, methods, status codes
2. **Socket Programming**: Low-level network communication in Java
3. **Concurrency**: Thread management, thread safety, synchronization
4. **Design Patterns**: Template Method, Repository, Service, Dependency Injection, Observer
5. **Web Architecture**: Layered architecture, separation of concerns
6. **Database Integration**: NoSQL operations, data persistence
7. **Real-time Communication**: Server-Sent Events for push notifications
8. **Security**: SSL/TLS, HTTPS configuration

---

## Additional Resources

- **HTTP/1.1 Specification**: https://tools.ietf.org/html/rfc2616
- **Server-Sent Events**: https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events
- **MongoDB Java Driver**: https://www.mongodb.com/docs/drivers/java/sync/current/
- **Foundation CSS**: https://get.foundation/sites/docs/

---

## License

Educational use only - DEE - FCT/UNL