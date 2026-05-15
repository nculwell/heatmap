# Convection Model

## Overview

The simulation models two heat transfer mechanisms: **conduction** (through solids and air at rest)
and **convection** (heat carried by moving air). Both are applied every time step (0.1 s).

---

## Conduction

For each cell, heat flows between it and each of its four neighbors according to Fourier's law,
discretized explicitly:

```
ΔT = conductivity_receiving × (T_neighbor − T_cell) × Δt
```

The **receiving cell's** conductivity governs the rate — a highly conductive cell absorbs heat
quickly; an insulating cell absorbs it slowly. Sinks hold their temperature fixed; sources enforce
a temperature floor.

---

## Convection

Convection is applied only to air cells, after the conduction step.

### Step 1 — Compute steady-state velocity

At each air cell, a velocity vector is derived from the local temperature gradient:

```
v = −α × ∇T
```

- `∇T` is approximated by central differences using the four neighbors.
- The negative sign means air flows **from hot toward cold** — the same direction as
  pressure-driven flow in a room (hot air expands, creating higher pressure, pushing
  air outward toward cooler, lower-pressure regions).
- `α` (= 0.05) is the convection strength constant; it converts a temperature gradient
  (°F/cell) into a velocity (cells/s).
- Velocity magnitude is clamped to 3 cells/s to keep the numerical scheme stable.

### Step 2 — Advect temperature

Temperature is transported along the velocity field using an **upwind finite-difference scheme**.
For a cell with velocity component `v` along a given axis:

```
if v > 0:  dT/dt = −v × (T_cell − T_upwind)
if v < 0:  dT/dt = −v × (T_downwind − T_cell)
```

The upwind side is whichever neighbor the air is flowing *from*. If that neighbor is not an air
cell (i.e., it is a solid wall, source, or sink), it is treated as a no-flow boundary: the
gradient is taken as zero and no advection occurs in that direction.

The temperature change applied each step is `dT/dt × Δt`.

### Numerical stability (CFL condition)

For the upwind scheme to be stable, the Courant–Friedrichs–Lewy (CFL) condition must hold:

```
|v| × Δt ≤ 1   (per axis, per cell)
```

With `Δt = 0.1 s` and `MAX_VELOCITY = 3 cells/s`:

```
CFL = 3 × 0.1 = 0.3   ✓
```

The maximum physically plausible gradient is roughly 52 °F/cell (source at 72 °F adjacent to
sink at 20 °F). At that gradient, the unclamped velocity would be `0.05 × 52 = 2.6 cells/s`,
so the cap is rarely reached.

---

## Approximations and Limitations

- **Steady-state velocity:** The velocity is recomputed from the current temperature field each
  step rather than being evolved with momentum. This captures the *direction* of convective flow
  but not inertial effects such as overshooting or vortex shedding.
- **2D floor plan:** Real convection is primarily vertical (hot air rises). In a horizontal floor
  plan there is no buoyancy axis, so the model approximates the net horizontal pressure-driven
  circulation that results from 3D convection — not the circulation itself.
- **No turbulence:** Turbulent mixing in real rooms greatly enhances heat transfer, especially at
  high temperature differences. This model uses laminar-flow advection only.

---

## Parameters

| Constant | Value | Meaning |
|---|---|---|
| `CONV_ALPHA` | 0.05 | Converts ∇T (°F/cell) to velocity (cells/s) |
| `MAX_VELOCITY` | 3.0 | Velocity cap for CFL stability (cells/s) |
| `DT` | 0.1 s | Time step |
