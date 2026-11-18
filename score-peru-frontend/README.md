# Score Perú — Frontend

Frontend Angular para consultar el score calculado por el backend Java y mostrar sus datos de contribuyente, reglas y sanciones.

## Ejecución local

1. Inicia el backend `score-peru` en `http://localhost:8080`.
2. Desde esta carpeta ejecuta:

   ```bash
   npm install
   npm start
   ```

3. Abre `http://localhost:4200`.

El servidor de desarrollo reenvía `/api/*` al backend Java para evitar problemas CORS. La URL base predeterminada del cliente es `/api/v1`; en un despliegue con otro host se puede definir `window.SCORE_PERU_API_URL` antes de cargar la aplicación.

## Integración usada

- `GET /api/v1/consulta/{ruc}`: reporte y score de riesgo.
- `GET /api/v1/dashboard/stats`: métricas nacionales agregadas.

El front presenta `puntajeRiesgo` y `nivelRiesgo` tal como los calcula el motor Java. La respuesta de caché del backend puede contener solo los campos base; en ese caso se muestra una nota y se mantienen vacíos los detalles no proporcionados.

El botón de PDF usa el diálogo de impresión del navegador (Guardar como PDF), ya que el backend no expone un endpoint de generación de PDF.
