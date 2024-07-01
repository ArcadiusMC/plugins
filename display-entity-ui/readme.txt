Steps to spawn a menu element, divided into sections.

=== Responsibilities ===
  RenderElement instances will handle the spawning (ie, rendering), 
  transformation and moving logic of each individual component. Each element 
  will need to know information about it's alignment, but no more than the size
  of child elements and where those elements are. Child elements are considered
  to be apart of the 'content' of an element, and thus padding and outline apply
  to them.

  Node (or AlignmentNode) instances will handle the aligning of each element.
  For the time being, each element will be rendered below the last one, in the 
  future additional rules about row or column alignment should be added to mimic
  the 'flex' display in HTML/CSS.

=== Steps: ===

== Section 1: Spawning (Per node) ==
  1.1 - Spawn content, if it's not empty, and store it in it's the render layer.
       Measure the content.

  1.2 - Spawn background, if it's set, and store in it's the render layer.

  1.3 - Spawn outline, if it's set, and store in it's the render layer.

  1.4 - Go through each layer, iterating from content layer upwards, and set
        it's size to be equal to the previous existing layer's size + layerSize
        (outlineSize or paddingSize). Content layer to be measured by
        this.content.measureContent.

  1.5 - Go through each layer, this time in the opposite order, and set it's
        offset on the X and Y axes to be equal to the border length. Assuming
        layers are aligned from the bottom left, use the bottom and left
        border/background sizes.

  1.6 - Offset content layer by half it's width. Text is always aligned to the
        middle of the entity, needs to be offset to force it to be consistent
        with bottom-left origin point.

  1.7 - Apply layer specific screen normal offset (Move each layer forward by a
        microscopic amount to prevent Z fighting) Content layer can be on the
        same level as the next layer, because text is programmed to always be
        above the background, so no Z fighting possible.

  1.8 - Apply screen's current rotation and translation to the created entities.
        Offset element by its height to move origin point from bottom-left to
        top-left

  1.9 - Apply transformations onto the entities themselves

== Section 2: Alignment (Per node) ==
  1.1 - Iterate over child nodes, calling the alignment function on them. If the
        current node has no children, stop. Alignment only matters when an
        element has children.

  1.2 - Find node's content location (the offset of the content layer + the
        node's position), call this C, and iterate over children again. Move the
        element to C, add the child's size.y to C and move on to next loop.