# NASSIaLART

Spring Boot application for displaying Pikud HaOref alerts for Ra'anana on a tablet, including a regular screen and `/kiosk` mode.

## Local Run

Requirements:
- Java 17
- Maven

Run:

```bash
mvn spring-boot:run
```

Open:

```text
http://localhost:8080
http://localhost:8080/kiosk
```

## Cloud Deployment

The project is now prepared for cloud deployment:
- `server.port` reads from `PORT`
- `Dockerfile` builds and runs the app
- `railway.toml` is included for Railway

### GitHub

Initialize git if needed:

```bash
git init
git add .
git commit -m "Initial deployable version"
```

Create a new empty GitHub repository, then connect it:

```bash
git remote add origin https://github.com/<YOUR_USER>/<YOUR_REPO>.git
git branch -M main
git push -u origin main
```

### Railway

1. Go to `https://railway.app`
2. Log in with GitHub
3. Choose `New Project`
4. Choose `Deploy from GitHub repo`
5. Select your repository
6. Railway will detect the `Dockerfile` and build automatically
7. After deploy, open the generated public domain

Kiosk URL:

```text
https://<your-railway-domain>/kiosk
```

### Important Note

This app depends on live Pikud HaOref endpoints. If their service blocks the hosting provider or changes behavior, the app may still run correctly while alert data becomes unavailable.

## Configuration

Main settings are in `src/main/resources/application.yml`:
- `app.alert.city`
- `app.alert.city-aliases`
- `app.alert.poll-interval-seconds`
- `app.alert.active-alert-retention-seconds`

## Tablet Usage

After deployment:
- open `/kiosk` on the tablet
- add it to the home screen
- keep the tablet connected to power
- disable screen sleep if needed
