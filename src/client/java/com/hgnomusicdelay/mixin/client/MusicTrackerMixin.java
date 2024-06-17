package com.hgnomusicdelay.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MusicTracker;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.MusicSound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MusicTracker.class)
public abstract class MusicTrackerMixin {
    @Shadow private int timeUntilNextSong;

    @Shadow @Final private MinecraftClient client;

    @Shadow @Nullable private SoundInstance current;

    @Shadow @Final private Random random;

    @Shadow public abstract void play(MusicSound type);

    @Inject(at = @At("HEAD"), method = "tick()V", cancellable = true)
    private void tick(CallbackInfo ci) {
        MusicSound musicSound = this.client.getMusicType();

        // Apparently music doesn't stop when volume is 0.0F, so we have to stop it manually
        if (this.current != null && client.options.getSoundVolume(SoundCategory.MUSIC) <= 0.0F) {
            this.client.getSoundManager().stop(this.current);
            this.timeUntilNextSong = MathHelper.nextInt((Random)this.random, (int)0, (int)(musicSound.getMinDelay() / 2));
            this.current = null;
        }

        // Force stop music if it should be replaced
        if (this.current != null && !((SoundEvent)musicSound.getSound().value()).getId().equals((Object)this.current.getId()) && musicSound.shouldReplaceCurrentMusic()) {
            this.client.getSoundManager().stop(this.current);
            this.timeUntilNextSong = MathHelper.nextInt((Random)this.random, (int)0, (int)(musicSound.getMinDelay() / 2));
            this.current = null; // force
        }

        if (this.current != null && !this.client.getSoundManager().isPlaying(this.current)) {
            this.current = null;
            this.timeUntilNextSong = Math.min(this.timeUntilNextSong, MathHelper.nextInt((Random)this.random, (int)musicSound.getMinDelay(), (int)musicSound.getMaxDelay()));
        }

        this.timeUntilNextSong = Math.min(this.timeUntilNextSong, musicSound.getMaxDelay());
        if (this.current == null) {// && this.timeUntilNextSong-- <= 0) {
            this.play(musicSound);
        }
        ci.cancel();
    }
}
