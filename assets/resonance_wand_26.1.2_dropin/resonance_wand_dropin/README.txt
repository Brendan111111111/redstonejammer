Resonance Wand texture + model files for Minecraft 26.1.2 (NeoForge)

HOW TO USE
==========

1. Open the folder: assets/MODID/

2. Rename the folder "MODID" to your actual mod ID 
   (the same namespace your Resonance Wand already uses).

3. Copy the entire contents into your mod's:
   src/main/resources/assets/<your_mod_id>/

   Final structure should look like:
   assets/
     <your_mod_id>/
       textures/item/resonance_wand.png
       models/item/resonance_wand.json
       items/resonance_wand.json

4. Rebuild / run the game.

NOTES
=====
- Texture is 16x16 (standard). A 32x32 version is also included if you prefer higher resolution.
- The model uses "handheld" so it holds correctly in the hand.
- The items/resonance_wand.json is required on 26.x / 1.21.4+.
- If your item is registered under a different name (e.g. resonating_wand), just rename the three files accordingly.
