# Convection Model

## Overview

The simulation models three heat transfer mechanisms applied every time step (0.1 s):

1. **Conduction** -- heat diffuses through solids and stationary air.
2. **Convection** -- heat is carried by moving air within each floor.
3. **Inter-floor transfer** -- heat passes between floors through the floor/ceiling and stairwell.

---

## Conduction

For each cell, heat flows between it and each of its four neighbors according to Fourier's law,
discretized explicitly:

```
dT = conductivity_receiving * (T_neighbor - T_cell) * dt
```

The **receiving cell's** conductivity governs the rate -- a highly conductive cell absorbs heat
quickly; an insulating cell absorbs it slowly. Sinks hold their temperature fixed; sources enforce
a temperature floor.

---

## Convection

Convection is applied to air and stairway cells after the conduction step. Each floor has its own
independent velocity field that persists between time steps.

### Velocity update

The velocity at each air cell is evolved using a buoyancy force and viscous damping:

```
v_new = DAMPING * v_old - BUOYANCY * grad(T) * dt
```

- `grad(T)` is approximated by central differences using the four neighbors.
- The negative sign means air accelerates **away from hot regions** -- pressure-driven outward
  flow, as hot air expands and pushes surrounding air toward cooler, lower-pressure areas.
- `DAMPING` (= 0.9) retains 90% of the velocity each step, modelling viscous dissipation.
- `BUOYANCY` (= 0.3) is the acceleration per unit temperature gradient (cells/s^2 per F/cell).
- Velocity magnitude is clamped to 3 cells/s for numerical stability.

Because velocity is stored between steps, it builds up coherently over time near temperature
gradients (momentum effects) and decays naturally as gradients shrink toward equilibrium.

### Temperature advection

Temperature is transported along the velocity field using an **upwind finite-difference scheme**.
For a cell with velocity component `v` along a given axis:

```
if v > 0:  dT/dt = -v * (T_cell - T_upwind)
if v < 0:  dT/dt = -v * (T_downwind - T_cell)
```

The upwind side is whichever neighbor the air is flowing *from*. Non-air neighbors (solid walls,
sources, sinks) act as no-flow boundaries: no advection occurs across them.

The temperature change applied each step is `dT/dt * dt`.

### Numerical stability (CFL condition)

For the upwind scheme to be stable, the Courant-Friedrichs-Lewy (CFL) condition must hold:

```
|v| * dt <= 1   (per axis, per cell)
```

With `dt = 0.1 s` and `MAX_VELOCITY = 3 cells/s`:

```
CFL = 3 * 0.1 = 0.3   (stable)
```

---

## Inter-floor Heat Transfer

Every cell pair (floor 1 cell directly below its floor 2 counterpart) exchanges heat each step.
The rate depends on cell type:

```
dT = conductivity * (T_other_floor - T_this_floor) * dt
```

| Cell type    | Conductivity | Represents                         |
|--------------|--------------|------------------------------------|
| Stairway (S) | 0.70         | Open air shaft, free heat exchange |
| All others   | 0.15         | Insulated floor/ceiling (wood)     |

Both floors are updated from their pre-step temperatures (explicit scheme, no ordering bias).

---

## Roof Heat Loss

Every floor-2 cell also loses heat upward through the roof to the outdoor sink temperature:

```
dT = ROOF_CONDUCTIVITY * (sinkTemp - T_cell) * dt
```

`ROOF_CONDUCTIVITY` (= 0.08) represents a moderately insulated roof. Sink and source cells
are unaffected (their temperatures are held fixed or floored by `withTemp`).

---

## Approximations and Limitations

- **2D floor plan:** Real convection is primarily vertical (hot air rises). In a horizontal floor
  plan there is no buoyancy axis, so the model approximates the net horizontal pressure-driven
  circulation that results from 3D convection -- not the circulation itself.
- **No divergence-free constraint:** The velocity field is not projected to be mass-conserving.
  This is an approximation; the primary effect is enhanced heat mixing rather than accurate
  flow patterns.
- **No turbulence:** Turbulent mixing in real rooms greatly enhances heat transfer. This model
  uses laminar-flow advection only.

---

## Parameters

| Constant              | Value  | Meaning                                                  |
|-----------------------|--------|----------------------------------------------------------|
| `CONV_BUOYANCY`       | 0.3    | Air acceleration per unit temperature gradient           |
| `CONV_DAMPING`        | 0.9    | Fraction of velocity retained per step (viscosity)       |
| `MAX_VELOCITY`        | 3.0    | Velocity cap for CFL stability (cells/s = 0.75 m/s)     |
| `FLOOR_CONDUCTIVITY`  | 0.15   | Heat transfer rate through floor/ceiling                 |
| `STAIR_CONDUCTIVITY`  | 0.70   | Heat transfer rate through stairwell                     |
| `ROOF_CONDUCTIVITY`   | 0.08   | Heat transfer rate through roof to outdoors              |
| `DT`                  | 0.1 s  | Time step                                                |
