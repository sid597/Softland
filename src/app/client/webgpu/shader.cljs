(ns app.client.webgpu.shader)




(def text-vertex-shader 
  (clj->js {:label "text vertex shader"
            :code "// vertex.wgsl
            // VertexInput example: [x1 y1 u1 v1] 
            // - [x1,y1] represent the coords in clipspace for triangle.
            // - [u1,v1] represent the coords in font atlas  

            struct VertexInput { 
               @location(0) position: vec2<f32>,
               @location(1) uv: vec2<f32>,
            };

            // Outputting: the position of the vertices and uv coords but why?

            struct VertexOutput {
               @builtin(position) position: vec4<f32>,
               @location(0) uv: vec2<f32>,
            };

            @vertex
            fn main(input: VertexInput) -> VertexOutput {
               var output: VertexOutput;
               output.position = vec4<f32>(input.position, 0.0, 1.0);
               output.uv = input.uv;
               return output;
               }
            "}))

(def text-fragment-shader
  (clj->js {:label "text fragment shader"
            :code "// fragment.wgsl
            @group(0) @binding(0)
            var sampler0: sampler;

            @group(0) @binding(1)
            var texture0: texture_2d<f32>;

            @group(0) @binding(2) var<uniform> sizes:sizing;

            struct sizing {
              pxRange: f32,
              atlasSize: f32,
              renderSize: f32,
            }


            fn median(a: f32, b: f32, c: f32) -> f32 {
                return max(min(a, b), min(max(a, b), c));
            }

            @fragment
            fn main(@location(0) uv: vec2<f32>) -> @location(0) vec4<f32> 
            {
             let msd = textureSample(texture0, sampler0, uv).rgb;
             let sd = median(msd.r, msd.g, msd.b);

             let pxRange = sizes.pxRange; 
             let atlasSize = sizes.atlasSize; 
             let renderSize = sizes.renderSize; 
             
             let screenPxRange = max(pxRange * (renderSize / atlasSize), 1.0);
             
             let screenPxDistance = screenPxRange * (sd - 0.5);
             let opacity = clamp(screenPxDistance + 0.5, 0.0, 1.0);
             
             let text = vec4<f32>(0.0, 0.0, 0.0, opacity); // Transparent background how??

             return text;
            }
            "}))


(def add-new-rects-shader-descriptor
  (clj->js {:label "vertices compute shader descriptor"
            :code  "
                     struct CanvasSettings {
                            width: f32,
                            height: f32,
                            panX: f32,
                            panY: f32,
                            zoomFactor: f32,
                     }
                     // Constants for screen dimensions
                     @group(0) @binding(0) var<storage, read>       rectangles: array<f32>;     // Flattened input array
                     @group(0) @binding(1) var<storage, read_write> vertices: array<f32>; // Output vertices
                     @group(0) @binding(2) var<uniform>             canvas_settings: CanvasSettings;  // Canvas settings uniform\n
                     @group(0) @binding(3) var<storage, read>       id_buffer: array<u32>;
                     @group(0) @binding(4) var<storage, read_write> rendered_ids: array<atomic<u32>>;
                      
                     fn clipX(x: f32, width: f32) -> f32 {
                         return (x / width) * 2.0 - 1.0;
                     }
                     
                     // Convert screen space Y coordinate to clip space
                     fn clipY(y: f32, height: f32) -> f32 {
                         return -((y / height) * 2.0 - 1.0); // Negative to flip Y axis

                     }


                     @compute @workgroup_size(64)
                     fn main(@builtin(global_invocation_id) global_id: vec3<u32>){
                      let index = global_id.x;
                      if (index >= arrayLength(&rectangles) / 4) {
                                            return;}
                      let base_index = index * 4 ;
                      let x          = rectangles[base_index]    ;
                      let y          = rectangles[base_index + 1];
                      let height     = rectangles[base_index + 2];
                      let width      = rectangles[base_index + 3];
                      let cwidth     = canvas_settings.width;
                      let cheight    = canvas_settings.height;

                      let z          = canvas_settings.zoomFactor;
                      let panX       = canvas_settings.panX;
                      let panY       = canvas_settings.panY;

                      let l          = (x            * z) + panX;
                      let r          = ((x + width)  * z) + panX;
                      let t          = (y            * z) + panY;
                      let b          = ((y + height) * z) + panY;

                      let left       = clipX(l,  cwidth);  
                      let right      = clipX(r,  cwidth); 
                      let top        = clipY(t, cheight);
                      let bottom     = clipY(b, cheight);



                      if (max(left, right) >= -1.0 && min(left, right) <= 1.0 && max(top, bottom) >= -1.0 && min(top, bottom) <= 1.0) {
                         let rect_id = id_buffer[index];
                         let pos = atomicAdd(&rendered_ids[0], 1u);
                         atomicStore(&rendered_ids[pos], rect_id);


                         // Create 6 vertices for two triangles (12 float values)
                         let vertex_index = index * 12;  // 6 vertices * 2 components each


                         // Triangle 1
                         vertices[vertex_index + 0] = left;
                         vertices[vertex_index + 1] = top;
                         vertices[vertex_index + 2] = right;
                         vertices[vertex_index + 3] = top;
                         vertices[vertex_index + 4] = left;
                         vertices[vertex_index + 5] = bottom;

                         // Triangle 2
                         vertices[vertex_index + 6] = right;
                         vertices[vertex_index + 7] = top;
                         vertices[vertex_index + 8] = right;
                         vertices[vertex_index + 9] = bottom;
                         vertices[vertex_index + 10] = left;
                         vertices[vertex_index + 11] = bottom;
                      }}

                      struct VertexOutput {
                      @builtin(position) position: vec4f,
                      @location(0) fragPos: vec2f
                      }

                      @group(0) @binding(1) var<storage, read>       vertex_buffer: array<f32>; // Changed to read-only


                      @vertex
                      fn renderVertices(@builtin(vertex_index) vertexIndex: u32) -> VertexOutput {
                         let index = vertexIndex * 2;
                         let px = vertex_buffer[index] ;
                         let py = vertex_buffer[index + 1];

                         var output: VertexOutput;
                         output.position = vec4f(px, py, 0.0, 1.0);
                         output.fragPos = vec2f(px, py);
                         return output;
                      }

                      @fragment
                      fn renderVerticesFragment(@location(0) fragPos: vec2<f32>) -> @location(0) vec4<f32> {
                          // Use both x and y coordinates for more varied colors
                          let r = sin(fragPos.x * 3.14159) * 0.3 + 0.9;
                          let g = cos(fragPos.y * 3.14159) * 0.3 + 0.5;
                          let b = sin((fragPos.x + fragPos.y) * 3.14159) * 0.3 + 0.5;


                          // Add some randomness to the color
                          let random = fract(sin(dot(fragPos, vec2(12.898, 78.233))) * 43758.5453);

                          // Mix the gradient colors with the random value
                          let color = mix(
                          vec4<f32>(r, g, b, 1.0),
                          vec4<f32>(random, random, random, 1.0),
                          0.1  // Adjust this value to control the amount of randomness (0.0 to 1.0)
                          );

                          return color;
                      }
                     "}))



(def simulation-shader-code 
 "
//////////////////////////////////////////////////////////////////////
// EXAMPLE WGSL FOR A MULTI-PASS FORCE-DIRECTED LAYOUT (COSMOS-STYLE)
//////////////////////////////////////////////////////////////////////

/////////////////////////////
// Common Data Structures
/////////////////////////////

// Each node has (x, y). We store them in a flat array<f32>, length = 2 * numNodes.
// Bind group #0, binding #0
@group(0) @binding(0)
var<storage, read> nodes : array<f32>; 

// We'll store final forces in a float buffer, length = 2 * numNodes.
// You can store them directly or use atomic<u32> if you prefer atomic accumulation.
// For simplicity, assume 32-bit float array for direct writing.
@group(0) @binding(1)
var<storage, read_write> outForces : array<f32>;

// We also assume a uniform struct with simulation params.
struct SimParams {
  numNodes        : u32,
  spaceSize       : f32,
  alpha           : f32,
  repulsion       : f32,
  theta           : f32,
  linkSpring      : f32,
  linkDistance    : f32,
  gravity         : f32,
  // etc., if needed
};
@group(0) @binding(2)
var<uniform> sim : SimParams;

// If we want link-based force, we can store adjacency in a buffer. 
// For each node i, we have adjacencyCount plus adjacencyIndices...
// This is just one possible layout, similar to the `linkInfoTexture` logic in Cosmos.
// We'll define a struct for adjacency info:

struct LinkInfo {
  // how many neighbors
  count   : u32,
  // index into the adjacency array
  offset  : u32,
};
@group(0) @binding(3)
var<storage, read> linkInfos : array<LinkInfo>;

// Then we have a big array of neighbor indices.
@group(0) @binding(4)
var<storage, read> linkIndices : array<u32>;

// For a multi-level quadtree, we might store multiple “levels” of center-of-mass data.
// We'll illustrate a single level first. Each cell has sumX, sumY, and count for that cell.
struct CoMCell {
  sumX  : atomic<u32>,   // we’ll store float bits via floatBitsToUint
  sumY  : atomic<u32>,
  count : atomic<u32>,   // integer count
};

// Suppose we have one such level with dimension levelSize × levelSize cells.
// In a real system, you might have an array of these for multiple levels.
@group(1) @binding(0)
var<storage, read_write> levelCoM : array<CoMCell>;

// We also pass in the dimension of this level (for indexing):
@group(1) @binding(1)
var<uniform> levelParams : vec2<f32>; 
// x = levelSize (number of cells in one dimension), y = not used or might store cellSize, etc.

// Or you might store each level’s dimension in an array if you have multiple levels.

/////////////////////////////
// Pass 1: Clear the CoM buffer
/////////////////////////////
// Typically we just set sumX,sumY,count = 0. 
// We can do that in a small compute pass that runs over [levelSize * levelSize] threads.

@compute @workgroup_size(64)
fn clearCoM(@builtin(global_invocation_id) global_id : vec3<u32>) {
  let idx = global_id.x;
  let levelSize = u32(levelParams.x);

  if (idx >= levelSize * levelSize) {
    return;
  }
  
  atomicStore(&levelCoM[idx].sumX, 0u);
  atomicStore(&levelCoM[idx].sumY, 0u);
  atomicStore(&levelCoM[idx].count, 0u);
}

/////////////////////////////
// Pass 2: Accumulate CoM for this level
/////////////////////////////
// Each thread processes one node, finds which cell it belongs to, does atomicAdd of sumX, sumY, and count.

@compute @workgroup_size(64)
fn calcCenterOfMassLevel(@builtin(global_invocation_id) global_id : vec3<u32>) {
  let i = global_id.x;  // node index
  if (i >= sim.numNodes) {
    return;
  }

  // read node position
  let base = i * 2u;
  let x = nodes[base];
  let y = nodes[base + 1u];

  // figure out which cell of the level we fall into
  let levelSizeF = levelParams.x;
  let spaceSize = sim.spaceSize;
  
  // clamp or assume x,y in [0, spaceSize], for example
  // you might do something else if the layout can go negative
  let cx = clamp(x, 0.0, spaceSize);
  let cy = clamp(y, 0.0, spaceSize);

  // map into [0, levelSize)
  let cellXf = floor(cx / spaceSize * levelSizeF);
  let cellYf = floor(cy / spaceSize * levelSizeF);

  let cellX = u32(cellXf);
  let cellY = u32(cellYf);

  let levelSz = u32(levelSizeF);
  if (cellX >= levelSz || cellY >= levelSz) {
    // out of range, skip
    return;
  }

  let cellIndex = cellY * levelSz + cellX;

  // atomicAdd to sumX,sumY, count
  // we store floats as bits
  let xbits = floatBitsToUint(x);
  let ybits = floatBitsToUint(y);

  atomicAdd(&levelCoM[cellIndex].sumX, xbits);
  atomicAdd(&levelCoM[cellIndex].sumY, ybits);
  atomicAdd(&levelCoM[cellIndex].count, 1u);
}

/////////////////////////////
// Pass 3: Repulsion from CoM
/////////////////////////////
// Each thread processes one node, scanning some or all cells in the level. 
// Or do a Barnes–Hut style approach: check if cell is far enough (using sim.theta).
// For simplicity, let’s do a naive approach: sum force from every cell that has count>0
// ignoring the “far enough” test. Real code might subdivide or skip recursion, etc.

@compute @workgroup_size(64)
fn calcRepulsionLevel(@builtin(global_invocation_id) global_id : vec3<u32>) {
  let i = global_id.x;
  if (i >= sim.numNodes) {
    return;
  }

  let base = i * 2u;
  let x = nodes[base];
  let y = nodes[base + 1u];

  var fx = 0.0;
  var fy = 0.0;

  let levelSizeF = levelParams.x;
  let levelSz = u32(levelSizeF);

  // We do a naive pass over all cells in [0, levelSz*levelSz). 
  // For a large grid, this can be expensive. But it shows the idea.
  // A real version might do a more advanced Barnes-Hut skip.

  for (var cellIndex = 0u; cellIndex < levelSz*levelSz; cellIndex = cellIndex + 1u) {
    let cCount = atomicLoad(&levelCoM[cellIndex].count);
    if (cCount == 0u) {
      continue; // empty cell
    }

    let sumX = atomicLoad(&levelCoM[cellIndex].sumX);
    let sumY = atomicLoad(&levelCoM[cellIndex].sumY);

    let massF = f32(cCount);

    let centerX = bitcast<f32>(sumX / cCount);
    let centerY = bitcast<f32>(sumY / cCount);

    // skip if it’s the same node or extremely close
    // (some tolerance so we don’t get inf)
    let dx = centerX - x;
    let dy = centerY - y;
    let distSqr = dx*dx + dy*dy + 0.0001; // avoid div zero
    let dist = sqrt(distSqr);

    // If you wanted a barnes-hut style check, you'd do:
    // if cellSize / dist < sim.theta { 
    //   // use approximation
    // } else {
    //   // subdivide or skip
    // }

    // Basic repulsion formula (like ForceAtlas2 / Fruchterman–Reingold style):
    // F ~ (repulsion * massOfCell) / dist^2
    let mag = sim.repulsion * massF / distSqr;
    // multiply by alpha
    mag = mag * sim.alpha;

    // accumulate
    fx += mag * dx / dist;
    fy += mag * dy / dist;
  }

  // Write repulsion force to outForces. 
  // If you have multiple passes (repulsion, link, gravity),
  // you might want to accumulate in outForces, so do e.g. outForces[base] += fx
  // but WGSL has no built-in atomicAdd for floats. One workaround is:
  //   - store partial forces in a separate float buffer, then sum later on CPU,
  //   - or do 32-bit atomics using bit patterns, etc.
  // For simplicity, let's directly write (overwriting). 
  // In practice, you'd want either an atomic approach or multi-buffer approach.

  outForces[base] = fx;
  outForces[base + 1u] = fy;
}

/////////////////////////////
// Pass 4: Link (Spring) Forces
/////////////////////////////
// Similar to your snippet from Cosmos: For each node, loop over that node’s neighbors
// in linkIndices to accumulate spring force.

@compute @workgroup_size(64)
fn calcLinkForces(@builtin(global_invocation_id) global_id : vec3<u32>) {
  let i = global_id.x;
  if (i >= sim.numNodes) {
    return;
  }

  let base = i * 2u;
  let x = nodes[base];
  let y = nodes[base + 1u];

  var fx = 0.0;
  var fy = 0.0;

  // how many neighbors does this node have?
  let info = linkInfos[i];
  let count = info.count;
  let offset = info.offset;

  for (var e = 0u; e < count; e = e + 1u) {
    let neighborIndex = linkIndices[offset + e];
    if (neighborIndex == i || neighborIndex >= sim.numNodes) {
      continue;
    }
    let nbBase = neighborIndex * 2u;
    let nx = nodes[nbBase];
    let ny = nodes[nbBase + 1u];

    // standard spring formula: 
    // F = k * (dist - linkDistance)
    let dx = nx - x;
    let dy = ny - y;
    let dist = sqrt(dx*dx + dy*dy + 0.00001);
    let desired = sim.linkDistance; // or with random variation if you like

    // “Spring force” magnitude:
    let stretch = dist - desired;
    let k = sim.linkSpring; 
    // multiply by alpha if you want damping
    let mag = k * stretch * sim.alpha;

    // direction
    fx += (mag * dx / dist);
    fy += (mag * dy / dist);
  }

  // Add to outForces or combine with existing. 
  // For simplicity, let's do a direct add to the outForces from the repulsion pass:
  outForces[base] = outForces[base] + fx;
  outForces[base + 1u] = outForces[base + 1u] + fy;
}

/////////////////////////////
// Pass 5 (Optional): Gravity / Center Pull
/////////////////////////////
// Just like in Cosmos, we might have a pass that pulls each node toward center.

@compute @workgroup_size(64)
fn calcGravity(@builtin(global_invocation_id) global_id : vec3<u32>) {
  let i = global_id.x;
  if (i >= sim.numNodes) {
    return;
  }

  let base = i * 2u;
  let x = nodes[base];
  let y = nodes[base + 1u];

  // Let center be (spaceSize/2, spaceSize/2)
  let cx = sim.spaceSize * 0.5;
  let cy = sim.spaceSize * 0.5;

  let dx = cx - x;
  let dy = cy - y;
  let dist = sqrt(dx*dx + dy*dy + 0.00001);

  // Some gravity constant:
  let g = sim.gravity; // or config.simulationGravity
  // multiply by alpha for damping
  let mag = g * dist * sim.alpha;

  // direction
  let fx = mag * (dx / dist);
  let fy = mag * (dy / dist);

  // accumulate
  outForces[base] = outForces[base] + fx;
  outForces[base + 1u] = outForces[base + 1u] + fy;
}

//////////////////////////////////////////////////////////////////////
// Usage Summary (Pseudo-code)
// 1) Clear CoM:        dispatch( (levelSize*levelSize + 63)/64, 1, 1 ) -> clearCoM
// 2) Calc CoM:         dispatch( (numNodes+63)/64, 1, 1 ) -> calcCenterOfMassLevel
// 3) Calc Repulsion:   dispatch( (numNodes+63)/64, 1, 1 ) -> calcRepulsionLevel
// 4) Calc Link Forces: dispatch( (numNodes+63)/64, 1, 1 ) -> calcLinkForces
// 5) Calc Gravity:     dispatch( (numNodes+63)/64, 1, 1 ) -> calcGravity
// 6) Then integrate positions in another pass, using outForces as velocity or acceleration.
//////////////////////////////////////////////////////////////////////
")
