# Stock Analysis Platform

A modern stock market analysis and research platform focused on **fundamental analysis**, **market insights**, **smart alerts**, and **interactive dashboards** for investors and traders.

---

#  Features

##  Dashboard & Market Overview
- Real-time market monitoring
- Interactive stock dashboard
- Sector-wise market coverage
- Market movers and trending stocks
- Custom analytics widgets

##  Fundamental Analysis
- Company financial overview
- EPS, PE Ratio, NAV, ROE analysis
- Revenue and profit growth tracking
- Historical financial comparison
- Intrinsic value analysis

##  Technical & Market Analysis
- Price trend analysis
- Volume analysis
- Support & resistance detection
- Breakout and momentum tracking
- Market sentiment indicators

##  Smart Alerts System
Get notified for important market movements:
- PE ratio unusually high
- Sudden volume spike
- Gap up / gap down
- Price breakout
- New yearly high
- Unusual market activity

##  Prediction & Research Tools
- Regression-based price prediction
- Historical trend analysis
- Statistical market analysis

##  Watchlist & Portfolio
- Save favorite stocks
- Portfolio tracking
- Performance monitoring
- Personalized research workspace


---

#  Tech Stack

## Backend
- Java
- Spring Boot
- PostgreSQL / H2 Database
- Thymeleaf

## Frontend
- HTML5
- CSS3
- Bootstrap
- JavaScript



---


# Environment Profiles

This project supports multiple Spring profiles.

## Available Profiles

| Profile | Description |
|----------|-------------|
| `postgres` | Local PostgreSQL database |
| `h2` | In-memory H2 database for testing |

---

## Configuration Files

```text
src/main/resources/
├── application.properties
├── application-postgres.properties
└── application-h2.properties
```

---

## Active Profile Configuration

Default profile is configured in:

```properties
spring.profiles.active=postgres
```

Located inside:

```text
src/main/resources/application.properties
```

---

## Switch Database Profile

### Use PostgreSQL

```properties
spring.profiles.active=postgres
```

### Use H2 Database

```properties
spring.profiles.active=h2
```

---

## PostgreSQL Setup

Configure database credentials inside:

```text
application-postgres.properties
```

Example:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/stockdb
spring.datasource.username=postgres
spring.datasource.password=password
```

---


#  Future Development

- AI-powered stock recommendations
- Advanced technical indicators
- Portfolio risk analysis
- Mobile responsive optimization
- Real-time notification system
- Machine learning price prediction
- DSE market integration improvements



