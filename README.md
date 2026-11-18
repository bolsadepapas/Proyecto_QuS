# Score_Perú

## Ejecución local

El backend Java y el frontend Angular se inician en terminales separadas. Ejecuta los comandos desde la raíz del repositorio (`Proyecto_QuS`).

### 1. Backend Java (puerto 8080)

Por defecto se activa el perfil `local` con base de datos H2 en memoria (no requiere instalar ni levantar PostgreSQL). En la primera terminal:

```bash
cd score-peru
./gradlew bootRun
```

Verifica el estado del backend en [http://localhost:8080/api/v1/health](http://localhost:8080/api/v1/health).

### 2. Frontend Angular (puerto 4200)

En una segunda terminal, desde `score-peru-frontend` (o directamente desde la raíz `Proyecto_QuS`):

```bash
cd score-peru-frontend
npm start
```

Abre [http://localhost:4200](http://localhost:4200). El servidor Angular reenvía automáticamente las llamadas `/api` al backend en `http://127.0.0.1:8080`.

### API QuS opcional (puerto 8000)

El backend Java usa el cliente simulado por defecto; para trabajar con ese modo no necesitas iniciar la API Python. Solo si configuraste `APP_CLIENTE_EXTERNO_MODO=rest`, inicia la API QuS en otra terminal:

```bash
cd "Api QuS"
./run.sh
```

`npm start` corresponde al frontend dentro de `score-peru-frontend`. `score-peru` es un proyecto Gradle/Java y se inicia con `./gradlew bootRun`. El único `run.sh` existente está dentro de `Api QuS`.

## Integrantes: 
- Johan Marquez Zuñiga
- Diego Nova Rosas
- Marcelo Silva Cabrera
- Facundo Sanz Palomino
- Luis Basurco Monroy
- Renzo Murillo Alvarez
