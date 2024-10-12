package net.arcadiusmc.bank;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.utils.io.ExistingObjectCodec;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.arcadiusmc.utils.io.JomlCodecs;
import org.joml.Vector3i;

@Getter @Setter
public class InnerVault {

  static final Codec<InnerVault> CODEC = ExistingObjectCodec.createCodec(
      InnerVault::new,
      builder -> {
        builder.optional("open-structure", ExtraCodecs.KEY_CODEC)
            .getter(InnerVault::getOpenVaultStructure)
            .setter(InnerVault::setOpenVaultStructure);

        builder.optional("closed-structure", ExtraCodecs.KEY_CODEC)
            .getter(InnerVault::getClosedVaultStructure)
            .setter(InnerVault::setClosedVaultStructure);

        builder.optional("place-position", JomlCodecs.VEC3I)
            .getter(InnerVault::getPosition)
            .setter(InnerVault::setPosition);

        builder.optional("code-positions", FacingPosition.CODEC.listOf())
            .getter(InnerVault::getCodePositions)
            .setter((vault, list) -> {
              vault.codePositions.clear();
              vault.codePositions.addAll(list);
            });

        builder.optional("code-length", Codec.intRange(1, 5))
            .getter(InnerVault::getCodeLength)
            .setter(InnerVault::setCodeLength);

        builder.optional("numpad-position", FullPosition.CODEC)
            .getter(InnerVault::getNumpadPosition)
            .setter((vault, fullPosition) -> vault.numpadPosition.set(fullPosition));
      }
  );

  private String openVaultStructure = null;
  private String closedVaultStructure = null;
  private Vector3i position = null;

  private final FullPosition numpadPosition = new FullPosition();

  private final List<FacingPosition> codePositions = new ObjectArrayList<>();
  private int codeLength = 4;
}
