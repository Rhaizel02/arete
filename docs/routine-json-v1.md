# Routine JSON v1

Routine JSON sirve para intercambiar **una rutina**. No es un backup de usuario y nunca incluirá sesiones, peso corporal ni preferencias.

## Contrato

```json
{
  "schemaVersion": 1,
  "name": "Torso Pierna 4 días",
  "workouts": [
    {
      "name": "Torso A",
      "notes": "Opcional",
      "exercises": [
        {
          "exercise": {
            "id": "arete-barbell-bench-press",
            "name": "Press de banca con barra",
            "nameEn": "Barbell Bench Press",
            "primaryMuscle": "Pecho",
            "secondaryMuscles": ["Tríceps", "Hombros"],
            "equipment": "Barra",
            "instructions": "Opcional"
          },
          "restSeconds": 180,
          "incrementKg": 2.5,
          "notes": "Pausa en el pecho",
          "groupId": "A1",
          "sets": [
            { "type": "WARMUP", "reps": { "exact": 5 } },
            { "type": "WORK", "reps": { "min": 8, "max": 12 } }
          ]
        }
      ]
    }
  ]
}
```

## Reglas v1

- `schemaVersion`, `name` y `workouts` son obligatorios.
- Cada workout necesita `name` y `exercises`.
- Cada elemento necesita una referencia de ejercicio con al menos `id`, `name` o `nameEn`, además de una o más `sets`.
- Una serie usa exactamente uno de estos modos: `{"exact": 10}` o `{"min": 8, "max": 12}`.
- `type` admite `WARMUP` y `WORK`; si se omite, es `WORK`.
- `restSeconds` vale 120 por defecto y acepta de 0 a 3600.
- Los campos opcionales se omiten en la exportación cuando no aportan información.
- Los campos desconocidos se ignoran para permitir extensiones compatibles.

## Resolución durante la importación

1. Coincidencia exacta por `exercise.id`.
2. Coincidencia sin distinguir mayúsculas por `exercise.name`.
3. Coincidencia sin distinguir mayúsculas por `exercise.nameEn`.
4. Alta como ejercicio personalizado, reutilizando el ID proporcionado cuando no colisiona.

La versión 1 se mantendrá aceptada cuando aparezcan versiones futuras. Una versión desconocida actualmente produce un error explícito antes de insertar datos; un ejercicio desconocido, en cambio, nunca invalida por sí solo la rutina.
