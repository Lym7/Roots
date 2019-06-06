package epicsquid.roots.item;

import epicsquid.mysticallib.item.ItemBase;
import epicsquid.mysticallib.particle.particles.ParticleGlitter;
import epicsquid.mysticallib.proxy.ClientProxy;
import epicsquid.mysticallib.util.Util;
import epicsquid.roots.capability.runic_shears.RunicShearsCapability;
import epicsquid.roots.capability.runic_shears.RunicShearsCapabilityProvider;
import epicsquid.roots.config.GeneralConfig;
import epicsquid.roots.init.ModRecipes;
import epicsquid.roots.recipe.RunicShearRecipe;
import epicsquid.roots.util.ItemSpawnUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCrops;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.IShearable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ItemRunicShears extends ItemBase {

  private Random random;

  public ItemRunicShears(@Nonnull String name) {
    super(name);
    setMaxDamage(80);
    setMaxStackSize(1);
    setHasSubtypes(false);
    random = new Random();
  }

  @Override
  @Nonnull
  public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    Block block = world.getBlockState(pos).getBlock();

    RunicShearRecipe recipe = ModRecipes.getRunicShearRecipe(block);

    if (recipe != null) {
      if (!world.isRemote) {

        if (block instanceof BlockCrops) {
          if (((BlockCrops) block).isMaxAge(world.getBlockState(pos))) {
            world.setBlockState(pos, ((BlockCrops) block).withAge(0));
          } else {
            return EnumActionResult.SUCCESS;
          }
        } else {
          world.setBlockState(pos, recipe.getReplacementBlock().getDefaultState());
        }
        ItemSpawnUtil.spawnItem(world, pos.add(0, 1, 0), recipe.getDrop().copy());
        if (!player.capabilities.isCreativeMode) {
          player.getHeldItem(hand).damageItem(1, player);
        }
      } else {
        for (int i = 0; i < 50; i++) {
          ClientProxy.particleRenderer.spawnParticle(world, Util.getLowercaseClassName(ParticleGlitter.class), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, random.nextDouble() * 0.1 * (random.nextDouble() > 0.5 ? -1 : 1), random.nextDouble() * 0.1 * (random.nextDouble() > 0.5 ? -1 : 1),
              random.nextDouble() * 0.1 * (random.nextDouble() > 0.5 ? -1 : 1), 120, 0.855 + random.nextDouble() * 0.05, 0.710, 0.943 - random.nextDouble() * 0.05, 1, random.nextDouble() + 0.5, random.nextDouble() * 2);
        }
      }
      world.playSound(player, pos, SoundEvents.ENTITY_SHEEP_SHEAR, SoundCategory.BLOCKS, 1f, 1f);
    }
    return EnumActionResult.SUCCESS;
  }

  @Override
  public boolean itemInteractionForEntity(ItemStack itemstack, EntityPlayer player, EntityLivingBase entity, EnumHand hand) {
    if (entity.world.isRemote) {
      return false;
    }

    Random rand = itemRand;

    if (entity instanceof IShearable) {
      int count = 0;
      entity.captureDrops = true;
      if (Items.SHEARS.itemInteractionForEntity(itemstack, player, entity, hand)) count++;
      entity.captureDrops = false;
      List<EntityItem> drops = new ArrayList<>(entity.capturedDrops);

      float radius = GeneralConfig.RunicShearsRadius;
      List<EntityLiving> entities = Util.getEntitiesWithinRadius(entity.world, (Entity e) -> e instanceof IShearable, entity.getPosition(), radius, radius / 2, radius);
      for (EntityLiving e : entities) {
        e.captureDrops = true;
        if (Items.SHEARS.itemInteractionForEntity(itemstack, player, e, hand)) count++;
        e.captureDrops = false;
        drops.addAll(entity.capturedDrops);
      }
      if (!drops.isEmpty()) {
        for (EntityItem ent : drops) {
          ent.posX = entity.posX;
          ent.posY = entity.posY;
          ent.posZ = entity.posZ;
          ent.motionY += rand.nextFloat() * 0.05F;
          ent.motionX += (rand.nextFloat() - rand.nextFloat()) * 0.1F;
          ent.motionZ += (rand.nextFloat() - rand.nextFloat()) * 0.1F;
          player.world.spawnEntity(ent);
        }
      }
      if (count > 0) return true;
      // ??? Return false?
    }

    RunicShearRecipe recipe = ModRecipes.getRunicShearRecipe(entity);
    if (recipe != null) {
      RunicShearsCapability cap = entity.getCapability(RunicShearsCapabilityProvider.RUNIC_SHEARS_CAPABILITY, null);
      if (cap != null) {
        if (cap.canHarvest()) {
          cap.setCooldown(recipe.getCooldown());
          net.minecraft.entity.item.EntityItem ent = entity.entityDropItem(recipe.getDrop().copy(), 1.0F);
          ent.motionY += rand.nextFloat() * 0.05F;
          ent.motionX += (rand.nextFloat() - rand.nextFloat()) * 0.1F;
          ent.motionZ += (rand.nextFloat() - rand.nextFloat()) * 0.1F;
          if (!player.capabilities.isCreativeMode) {
            itemstack.damageItem(1, entity);
          }
          // TODO: play particles
          // TODO: play noise
          return true;
        } else {
          // TODO: play particles (failure)
          // TODO: send message

        }
      }
    }

    return false;
  }
}
