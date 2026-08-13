Redstone Jammer - Creative Tab Icon
Namespace: redstonejammer

This is a clean 16x16 version of the sci-fi wand, suitable as the creative tab icon.

Recommended usage in NeoForge (26.1.2):

In your creative tab registration:

CreativeModeTab.builder()
    .title(Component.translatable("itemGroup.redstonejammer"))
    .icon(() -> new ItemStack(ModItems.RESONANCE_WAND.get()))  // or whatever your wand item is
    .displayItems((params, output) -> {
        // add your items here
    })
    .build();

If you prefer a dedicated texture instead of an existing item, you can still
point the icon to an item that uses this texture.
