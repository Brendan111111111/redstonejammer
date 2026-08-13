Overview
Redstone Jammer is a focused technical utility mod that lets you wirelessly jam, suppress, and force-off redstone mechanisms. Doors, gates, trapdoors, redstone lamps, powered rails, observers, and other signal-driven blocks can be remotely forced closed, unpowered, or disabled for a timed duration or continuously while a projector is active.
The mod is designed for base security, trap systems, redstone defense countermeasures, and creative “blackout” setups where you want to shut down mechanisms without physically breaking them or running wires to every target.

Features
Flux Inversion Projector
A placeable block that acts as a continuous wireless jammer.

Power it with normal redstone.
Use the Resonance Disruptor Wand to select the projector, then link distant target blocks.
While powered, it continuously forces linked mechanisms into their “off” state (doors closed, power levels zeroed, lamps extinguished, etc.).
Visual flux-laser particles show the active links.
Automatically detects and jams connected redstone structures (doors, gates, wires, etc.).

Chrono-Pulse Suppressor
A configurable timed pulse jammer.

Open its GUI to set a custom unit name (or cycle through presets such as Alpha Unit, Bravo Unit, Charlie Unit, Vault Guard, Perimeter Jammer, Redstone Core, Main Gate, Secret Safehouse, Sub-Level Jammer).
Adjust pulse duration (1–120 seconds) and pulse radius (1–16 blocks).
Link specific targets or rely on the area radius.
Fires a temporary electromagnetic jam that suppresses mechanisms for the chosen duration.
Fully controllable from the Sub-Frequency Stealth Remote.

Resonance Disruptor Wand
The primary linking tool.

Right-click a Flux Inversion Projector to bind/select it.
Right-click a target block to link it.
Shift + right-click to unlink.
Clear in-game feedback messages confirm every action.

Sub-Frequency Stealth Remote
A personal remote control for Chrono-Pulse Suppressors.

Right-click a suppressor to register it to your remote (each player maintains their own list).
Open the remote GUI to:
– Cycle between registered suppressors
– Fire the currently selected unit’s pulse
– Trigger Mass Overload (activate every linked suppressor at once)
Allows completely remote, stealthy activation of jamming pulses without standing next to the devices.


How Jamming Works
When a block is jammed, the mod:

Tracks the target and any connected redstone/door/gate structure.
Continuously forces relevant blockstate properties (powered, open, lit, active, enabled, power/signal level, etc.) to their off/zero values.
Re-applies the forced state every tick and intercepts neighbor updates so the mechanism stays disabled for the duration of the jam.
Displays a message when a player tries to interact with a jammed mechanism: “Mechanism is jammed by electromagnetic interference!”

The effect is temporary (timed pulses) or continuous (while a Flux Projector remains powered). Removing power or the end of a pulse restores normal redstone behavior.

Ideal Use Cases

Secure vaults and bases that can be remotely locked down
Trap corridors that disable themselves on command
Countering other players’ redstone door/gate systems
Creating “blackout” zones or timed lockdown sequences
Aesthetic or roleplay electromagnetic-warfare setups


Creative Tab
All items and blocks appear in the Redstone Jammer Countermeasures creative tab.
