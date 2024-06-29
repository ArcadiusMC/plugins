Steps to spawn a menu element, divided into sections.

=== Steps: ===
== Section 1: Measuring ==

  1.1 - Go through child elements and execute the measuring step on
        them.

  1.2 - Go through each layer, iterating from content layer upwards,
        and set it's size to be equal to the previous existing layer's
        size + layerSize (outlineSize or paddingSize). Content layer
        to be measured by this.content.measureContent.

  1.3 - Iterate over child elements, align their positions in
        accordance with alignment rules (Currently just place each
        element under the last)

== Section 2: Spawning ==

  2.1 - Spawn content, if it's not empty, and store it in it's the render
        layer

  2.2 - Spawn background, if it's set, and store in it's the render layer.

  2.3 - Spawn outline, if it's set, and store in it's the render layer.

  2.4 - Go through each layer, this time in the opposite order, and
        set it's offset on the X and Y axes to be equal to the border
        length. Assuming layers are aligned from the bottom left, use
        the bottom and left border/background sizes.

  2.5 - Offset content layer by half it's width. Text is always aligned
        to the middle of the entity, needs to be offset to force it to
        be consistent with bottom-left origin point.

  2.6 - Apply layer specific screen normal offset (Move each layer
        forward by a microscopic amount to prevent Z fighting) Content
        layer can be on the same level as the next layer, because text
        is programmed to always be above the background, so no Z fighting
        possible.

  2.7 - Apply screen's current rotation and translation to the created
        entities. Offset element by its height to move origin point
        from bottom-left to top-left

  2.8 - Apply transformations onto the entities themselves

  2.9 - Spawn child elements