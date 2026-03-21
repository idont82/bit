
import * as THREE from 'three';

// ─── State ───
const State = {
  IDLE: 0,
  MOVING: 1,
  DROPPING: 2,
  GRABBING: 3,
  RISING: 4,
  RETURNING: 5,
  RESULT: 6
};

let state = State.IDLE;
let credits = 3, score = 0, timer = 0, timerInterval = null;
let craneX = 0, craneZ = 0;       // -1 ~ 1 normalized
let craneTargetX = 0, craneTargetZ = 0;
let wireY = 0;                     // 0 = up, 1 = fully down
let clawOpen = 1;                  // 1 = open, 0 = closed
let pendulumAngleX = 0, pendulumAngleZ = 0;
let pendulumVelX = 0, pendulumVelZ = 0;
let craneVelX = 0, craneVelZ = 0;
let grabbedPlushie = null;
let inputDir = {x:0, z:0};
const CRANE_SPEED = 0.018;
const MOVE_BOUNDS = 0.85;
const DROP_SPEED = 0.012;
const RISE_SPEED = 0.008;
const PENDULUM_DAMPING = 0.92;
const PENDULUM_GRAVITY = 0.004;
const PENDULUM_IMPULSE = 0.08;
const GRAB_PROBABILITY = 0.45;
const DROP_ZONE = {x: 0.85, z: 0.55}; // return toward prize hole
// Prize hole position (right wall flush) — BOX_W/2=1.8, holeW=0.975
const HOLE_POS = {x: 1.8 - 0.975/2 - 0.05, z: 0.8};
const HOLE_RADIUS = 0.35;
// Circular joystick tracking
let prevInputAngle = 0, inputAngularVel = 0;

// ─── Three.js Setup ───
const viewport = document.getElementById('viewport');
const scene = new THREE.Scene();
scene.background = new THREE.Color(0x2a2a3a);
scene.fog = new THREE.Fog(0x2a2a3a, 10, 20);

const camera = new THREE.PerspectiveCamera(40, 4/3.5, 0.1, 50);
camera.position.set(0, 4.8, 6.5);
camera.lookAt(0, 1.5, 0);

const renderer = new THREE.WebGLRenderer({antialias: true, alpha: false});
renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
renderer.shadowMap.enabled = true;
renderer.shadowMap.type = THREE.PCFSoftShadowMap;
renderer.toneMapping = THREE.ACESFilmicToneMapping;
renderer.toneMappingExposure = 1.5;
viewport.appendChild(renderer.domElement);

function resizeRenderer(){
  const rect = viewport.getBoundingClientRect();
  renderer.setSize(rect.width, rect.height);
  camera.aspect = rect.width / rect.height;
  camera.updateProjectionMatrix();
}
resizeRenderer();
window.addEventListener('resize', resizeRenderer);

// ─── Lighting (bright interior) ───
const ambLight = new THREE.AmbientLight(0x8899bb, 1.2);
scene.add(ambLight);

const topLight = new THREE.PointLight(0xffffff, 2.5, 14);
topLight.position.set(0, 4.5, 0);
topLight.castShadow = true;
topLight.shadow.mapSize.set(512,512);
scene.add(topLight);

// Extra interior fill lights
const fillLight1 = new THREE.PointLight(0xffffff, 1.2, 8);
fillLight1.position.set(-1, 2.5, 1);
scene.add(fillLight1);
const fillLight2 = new THREE.PointLight(0xffffff, 1.2, 8);
fillLight2.position.set(1, 2.5, 1);
scene.add(fillLight2);

const frontLight = new THREE.DirectionalLight(0xccddff, 0.8);
frontLight.position.set(0, 3, 5);
scene.add(frontLight);

// LED glow lights
const ledColors = [0xff0044, 0x00ccff, 0xff00ff, 0x00ff88];
ledColors.forEach((c,i)=>{
  const l = new THREE.PointLight(c, 0.5, 5);
  l.position.set(-1.5 + i, 4.2, -1.5 + i*0.3);
  scene.add(l);
});

// ─── Box / Environment (based on blueprint: 900x850x2050mm) ───
const BOX_W = 3.6, BOX_H = 3.8, BOX_D = 3.2;
const PLAY_W = BOX_W;  // full width — one open space
const PLAY_OFFSET = 0;

// Floor
const floorGeo = new THREE.PlaneGeometry(BOX_W, BOX_D);
const floorMat = new THREE.MeshStandardMaterial({color:0x334466, roughness:0.7});
const floor = new THREE.Mesh(floorGeo, floorMat);
floor.rotation.x = -Math.PI/2;
floor.receiveShadow = true;
scene.add(floor);

// Floor grid
const gridHelper = new THREE.GridHelper(Math.max(BOX_W,BOX_D), 12, 0x3355aa, 0x263050);
gridHelper.position.y = 0.005;
scene.add(gridHelper);

// ─── Prize Drop Hole (rectangular with raised walls) ───
const holeW = 0.975, holeD = 1.125; // 1.5x bigger
const wallH = 0.65; // 1.3x higher
const wallT = 0.05; // wall thickness

// Black hole floor (bottom of chute)
const holeGeo = new THREE.PlaneGeometry(holeW, holeD);
const holeMat = new THREE.MeshStandardMaterial({color:0x030306, roughness:1});
const holeMesh = new THREE.Mesh(holeGeo, holeMat);
holeMesh.rotation.x = -Math.PI/2;
holeMesh.position.set(HOLE_POS.x, -0.01, HOLE_POS.z);
scene.add(holeMesh);

// Raised walls around the hole
const rimMat = new THREE.MeshPhysicalMaterial({color:0xaaccee, transparent:true, opacity:0.25, metalness:0.3, roughness:0.1, side:THREE.DoubleSide});
const rimInnerMat = new THREE.MeshPhysicalMaterial({color:0x88aacc, transparent:true, opacity:0.15, roughness:0.1, side:THREE.DoubleSide});
const holeRimGroup = new THREE.Group();

// Front wall
const wallFrontOuter = new THREE.Mesh(new THREE.BoxGeometry(holeW + wallT*2, wallH, wallT), rimMat);
wallFrontOuter.position.set(0, wallH/2, holeD/2 + wallT/2);
holeRimGroup.add(wallFrontOuter);
// Back wall
const wallBackOuter = new THREE.Mesh(new THREE.BoxGeometry(holeW + wallT*2, wallH, wallT), rimMat);
wallBackOuter.position.set(0, wallH/2, -holeD/2 - wallT/2);
holeRimGroup.add(wallBackOuter);
// Left wall
const wallLeftOuter = new THREE.Mesh(new THREE.BoxGeometry(wallT, wallH, holeD), rimMat);
wallLeftOuter.position.set(-holeW/2 - wallT/2, wallH/2, 0);
holeRimGroup.add(wallLeftOuter);
// Right wall
const wallRightOuter = new THREE.Mesh(new THREE.BoxGeometry(wallT, wallH, holeD), rimMat);
wallRightOuter.position.set(holeW/2 + wallT/2, wallH/2, 0);
holeRimGroup.add(wallRightOuter);

// Inner dark faces (inside the chute)
const innerFront = new THREE.Mesh(new THREE.PlaneGeometry(holeW, wallH), rimInnerMat);
innerFront.position.set(0, wallH/2, holeD/2);
innerFront.rotation.y = Math.PI;
holeRimGroup.add(innerFront);
const innerBack = new THREE.Mesh(new THREE.PlaneGeometry(holeW, wallH), rimInnerMat);
innerBack.position.set(0, wallH/2, -holeD/2);
holeRimGroup.add(innerBack);
const innerLeft = new THREE.Mesh(new THREE.PlaneGeometry(holeD, wallH), rimInnerMat);
innerLeft.position.set(-holeW/2, wallH/2, 0);
innerLeft.rotation.y = Math.PI/2;
holeRimGroup.add(innerLeft);
const innerRight = new THREE.Mesh(new THREE.PlaneGeometry(holeD, wallH), rimInnerMat);
innerRight.position.set(holeW/2, wallH/2, 0);
innerRight.rotation.y = -Math.PI/2;
holeRimGroup.add(innerRight);

// Top rim (metal trim on top of walls)
const trimMat = new THREE.MeshPhysicalMaterial({color:0xccddee, transparent:true, opacity:0.35, metalness:0.6, roughness:0.1});
const trimH = 0.02;
const trimFront = new THREE.Mesh(new THREE.BoxGeometry(holeW + wallT*2 + 0.02, trimH, wallT + 0.02), trimMat);
trimFront.position.set(0, wallH + trimH/2, holeD/2 + wallT/2);
holeRimGroup.add(trimFront);
const trimBack = new THREE.Mesh(new THREE.BoxGeometry(holeW + wallT*2 + 0.02, trimH, wallT + 0.02), trimMat);
trimBack.position.set(0, wallH + trimH/2, -holeD/2 - wallT/2);
holeRimGroup.add(trimBack);
const trimLeft = new THREE.Mesh(new THREE.BoxGeometry(wallT + 0.02, trimH, holeD + 0.02), trimMat);
trimLeft.position.set(-holeW/2 - wallT/2, wallH + trimH/2, 0);
holeRimGroup.add(trimLeft);
const trimRight = new THREE.Mesh(new THREE.BoxGeometry(wallT + 0.02, trimH, holeD + 0.02), trimMat);
trimRight.position.set(holeW/2 + wallT/2, wallH + trimH/2, 0);
holeRimGroup.add(trimRight);

holeRimGroup.position.set(HOLE_POS.x, 0, HOLE_POS.z);
scene.add(holeRimGroup);

// ─── Walls ───
// Back wall
const wallGeo = new THREE.PlaneGeometry(BOX_W, BOX_H);
const wallMat = new THREE.MeshStandardMaterial({color:0x2a3860, roughness:0.8});
const backWall = new THREE.Mesh(wallGeo, wallMat);
backWall.position.set(0, BOX_H/2, -BOX_D/2);
scene.add(backWall);

// Side walls (transparent acrylic)
const sideWallMat = new THREE.MeshPhysicalMaterial({
  color:0xaabbdd, transparent:true, opacity:0.06,
  roughness:0.05, metalness:0.1, side: THREE.DoubleSide
});
const leftWall = new THREE.Mesh(new THREE.PlaneGeometry(BOX_D, BOX_H), sideWallMat);
leftWall.position.set(-BOX_W/2, BOX_H/2, 0);
leftWall.rotation.y = Math.PI/2;
scene.add(leftWall);

const rightWall = new THREE.Mesh(new THREE.PlaneGeometry(BOX_D, BOX_H), sideWallMat);
rightWall.position.set(BOX_W/2, BOX_H/2, 0);
rightWall.rotation.y = -Math.PI/2;
scene.add(rightWall);

// Front acrylic (very faint)
const frontWall = new THREE.Mesh(
  new THREE.PlaneGeometry(BOX_W, BOX_H),
  new THREE.MeshPhysicalMaterial({
    color:0xaabbdd, transparent:true, opacity:0.03,
    roughness:0.05, metalness:0.05, side: THREE.DoubleSide
  })
);
frontWall.position.set(0, BOX_H/2, BOX_D/2);
scene.add(frontWall);

// ─── Main Frame Structure (metal corner posts) ───
const frameMat = new THREE.MeshStandardMaterial({color:0xaaaaaa, metalness:0.9, roughness:0.2});
const frameR = 0.04;
[[-BOX_W/2, -BOX_D/2],[-BOX_W/2, BOX_D/2],[BOX_W/2,-BOX_D/2],[BOX_W/2,BOX_D/2]].forEach(([x,z])=>{
  const post = new THREE.Mesh(new THREE.CylinderGeometry(frameR, frameR, BOX_H, 8), frameMat);
  post.position.set(x, BOX_H/2, z);
  scene.add(post);
});
// Top horizontal frame bars
[[0, -BOX_D/2],[0, BOX_D/2]].forEach(([x,z])=>{
  const bar = new THREE.Mesh(new THREE.BoxGeometry(BOX_W, 0.05, 0.05), frameMat);
  bar.position.set(x, BOX_H, z);
  scene.add(bar);
});
[[-BOX_W/2, 0],[BOX_W/2, 0]].forEach(([x,z])=>{
  const bar = new THREE.Mesh(new THREE.BoxGeometry(0.05, 0.05, BOX_D), frameMat);
  bar.position.set(x, BOX_H, z);
  scene.add(bar);
});

// ─── Rails (full width) ───
const railMat = new THREE.MeshStandardMaterial({color:0x888888, metalness:0.8, roughness:0.3});

const xRailGeo = new THREE.BoxGeometry(BOX_W - 0.2, 0.06, 0.06);
const xRail1 = new THREE.Mesh(xRailGeo, railMat);
xRail1.position.set(0, BOX_H - 0.15, -BOX_D/2 + 0.2);
scene.add(xRail1);
const xRail2 = new THREE.Mesh(xRailGeo, railMat);
xRail2.position.set(0, BOX_H - 0.15, BOX_D/2 - 0.2);
scene.add(xRail2);

// Z-axis rail (moves along X)
const zRailGeo = new THREE.BoxGeometry(0.06, 0.06, BOX_D - 0.3);
const zRail = new THREE.Mesh(zRailGeo, railMat);
zRail.position.set(0, BOX_H - 0.1, 0);
scene.add(zRail);

// ─── Crane Group ───
const craneGroup = new THREE.Group();
scene.add(craneGroup);

// Motor box (red housing)
const motorGeo = new THREE.BoxGeometry(0.5, 0.3, 0.5);
const motorMat = new THREE.MeshStandardMaterial({color:0xcc2222, metalness:0.6, roughness:0.4});
const motorBox = new THREE.Mesh(motorGeo, motorMat);
craneGroup.add(motorBox);
// Motor detail — cable drum
const drumGeo = new THREE.CylinderGeometry(0.08, 0.08, 0.35, 12);
const drumMat = new THREE.MeshStandardMaterial({color:0x555555, metalness:0.8, roughness:0.3});
const drum = new THREE.Mesh(drumGeo, drumMat);
drum.rotation.z = Math.PI/2;
drum.position.set(0, -0.1, 0);
craneGroup.add(drum);

// Wire (braided cable look)
const wireGeo = new THREE.CylinderGeometry(0.02, 0.02, 1, 8);
const wireMat = new THREE.MeshStandardMaterial({color:0x888888, metalness:0.9, roughness:0.2});
const wire = new THREE.Mesh(wireGeo, wireMat);
wire.position.y = -0.6;
craneGroup.add(wire);

// Claw pivot (for pendulum)
const clawPivot = new THREE.Group();
craneGroup.add(clawPivot);

// === Realistic Claw Assembly ===
const metalMat = new THREE.MeshStandardMaterial({color:0xbbbbbb, metalness:0.85, roughness:0.2});
const redMat = new THREE.MeshStandardMaterial({color:0xcc3333, metalness:0.6, roughness:0.35});
const darkMat = new THREE.MeshStandardMaterial({color:0x444444, metalness:0.8, roughness:0.3});

// Claw hub (central cylinder with ring)
const hubGeo = new THREE.CylinderGeometry(0.12, 0.14, 0.18, 12);
const hub = new THREE.Mesh(hubGeo, metalMat);
clawPivot.add(hub);
// Hub ring
const hubRingGeo = new THREE.TorusGeometry(0.15, 0.025, 8, 16);
const hubRing = new THREE.Mesh(hubRingGeo, darkMat);
hubRing.rotation.x = Math.PI/2;
hubRing.position.y = -0.05;
clawPivot.add(hubRing);

// 3 Claw prongs — curved hook shape
const fingers = [];
for(let i = 0; i < 3; i++){
  const angle = (i / 3) * Math.PI * 2;
  const prongGroup = new THREE.Group();
  prongGroup.userData.baseAngle = angle;

  // Upper arm (connects to hub)
  const upperGeo = new THREE.BoxGeometry(0.05, 0.22, 0.07);
  const upper = new THREE.Mesh(upperGeo, metalMat);
  upper.position.y = -0.18;
  prongGroup.add(upper);

  // Joint (pivot ball)
  const jointGeo = new THREE.SphereGeometry(0.04, 8, 8);
  const joint = new THREE.Mesh(jointGeo, darkMat);
  joint.position.y = -0.3;
  prongGroup.add(joint);

  // Curved hook prong (TubeGeometry along CatmullRom curve)
  const hookPts = [
    new THREE.Vector3(0, 0, 0),
    new THREE.Vector3(0, -0.06, 0),
    new THREE.Vector3(0, -0.12, 0.008),
    new THREE.Vector3(0, -0.18, 0.03),
    new THREE.Vector3(0, -0.23, 0.07),
    new THREE.Vector3(0, -0.26, 0.12),
    new THREE.Vector3(0, -0.27, 0.16),
    new THREE.Vector3(0, -0.255, 0.19),
  ];
  const hookCurve = new THREE.CatmullRomCurve3(hookPts);
  const hookGeo = new THREE.TubeGeometry(hookCurve, 24, 0.024, 8, false);
  const hook = new THREE.Mesh(hookGeo, redMat);
  hook.position.y = -0.3;
  prongGroup.add(hook);

  // Hook tip cap (rounded)
  const tipGeo = new THREE.SphereGeometry(0.028, 8, 6);
  const tip = new THREE.Mesh(tipGeo, redMat);
  tip.position.set(0, -0.555, 0.19);
  prongGroup.add(tip);

  // Inner grip pad (rubber-like, on inner curve)
  const padGeo = new THREE.BoxGeometry(0.035, 0.06, 0.025);
  const padMat = new THREE.MeshStandardMaterial({color:0x222222, roughness:0.95});
  const pad = new THREE.Mesh(padGeo, padMat);
  pad.position.set(0, -0.54, 0.13);
  pad.rotation.x = 0.4;
  prongGroup.add(pad);

  // Position around hub
  prongGroup.position.set(Math.cos(angle)*0.1, -0.05, Math.sin(angle)*0.1);
  fingers.push(prongGroup);
  clawPivot.add(prongGroup);
}

craneGroup.position.set(0, BOX_H - 0.2, 0);

// ─── Floor Position Indicator (십자선 + 링) ───
const indicatorGroup = new THREE.Group();
indicatorGroup.position.y = 0.02;
scene.add(indicatorGroup);

// Glowing ring
const ringGeo = new THREE.RingGeometry(0.28, 0.35, 32);
const ringMat = new THREE.MeshBasicMaterial({color:0x44aaff, transparent:true, opacity:0.5, side:THREE.DoubleSide});
const ring = new THREE.Mesh(ringGeo, ringMat);
ring.rotation.x = -Math.PI/2;
indicatorGroup.add(ring);

// Crosshair lines
const crossMat = new THREE.MeshBasicMaterial({color:0x44aaff, transparent:true, opacity:0.4});
const crossH = new THREE.Mesh(new THREE.PlaneGeometry(0.7, 0.025), crossMat);
crossH.rotation.x = -Math.PI/2;
indicatorGroup.add(crossH);
const crossV = new THREE.Mesh(new THREE.PlaneGeometry(0.025, 0.7), crossMat);
crossV.rotation.x = -Math.PI/2;
indicatorGroup.add(crossV);

// Center dot
const dotGeo = new THREE.CircleGeometry(0.05, 12);
const dotMat = new THREE.MeshBasicMaterial({color:0xffffff, transparent:true, opacity:0.6});
const dot = new THREE.Mesh(dotGeo, dotMat);
dot.rotation.x = -Math.PI/2;
dot.position.y = 0.005;
indicatorGroup.add(dot);

// ─── Plushie Creation ───
const plushies = [];
const plushieGroup = new THREE.Group();
scene.add(plushieGroup);

const PLUSHIE_THEMES = {
  shinchan: [
    {name:'짱구', skinColor:0xFFD4A0, shirtColor:0xFF2233, pantsColor:0xFFFF33, hairColor:0x111111, char:'shin'},
    {name:'짱아', skinColor:0xFFD4A0, shirtColor:0xFFFF33, pantsColor:0xFFFF33, hairColor:0xCC6622, char:'himawari'}
  ],
  cinnamoroll: [
    {name:'시나모롤', bodyColor:0xFFFFFF, bellyColor:0xFFFFFF, accentColor:0x88CCFF, char:'cinna', ribbonColor:0xFF88BB},
    {name:'모카', bodyColor:0xC89664, bellyColor:0xE8C8A0, accentColor:0x885533, char:'mocha', ribbonColor:0xFF6688},
    {name:'카푸치노', bodyColor:0xFFF0DD, bellyColor:0xFFFFFF, accentColor:0xDDAA88, char:'cappuccino', ribbonColor:0x88DDFF},
    {name:'에스프레소', bodyColor:0x8B5E3C, bellyColor:0xC49A6C, accentColor:0x553322, char:'espresso', ribbonColor:0xFFCC44},
    {name:'밀크', bodyColor:0xFFF8F0, bellyColor:0xFFFFFF, accentColor:0xFFBBDD, char:'milk', ribbonColor:0xFFAACC},
    {name:'시폰', bodyColor:0xFFE8DD, bellyColor:0xFFF5EE, accentColor:0xFFAAAA, char:'chiffon', ribbonColor:0xAADDFF}
  ],
  dino: [
    {name:'티라노', bodyColor:0x44AA44, bellyColor:0xAADD88, accentColor:0x338833, char:'trex', spineColor:0x226622},
    {name:'트리케라', bodyColor:0x6688CC, bellyColor:0xAABBDD, accentColor:0x445588, char:'trice', spineColor:0x334466},
    {name:'브라키오', bodyColor:0xCCAA44, bellyColor:0xEEDD88, accentColor:0x998833, char:'brachi', spineColor:0x776622},
    {name:'스테고', bodyColor:0xCC6644, bellyColor:0xEEAA88, accentColor:0x994433, char:'stego', spineColor:0x883322},
    {name:'프테라', bodyColor:0x8866CC, bellyColor:0xBBAAdd, accentColor:0x664488, char:'ptera', spineColor:0x553377},
    {name:'아기공룡', bodyColor:0x66CC88, bellyColor:0xAAEEBB, accentColor:0x44AA66, char:'baby', spineColor:0x339955}
  ],
  maenggu: [
    {name:'맹구', skinColor:0xFFD4A0, shirtColor:0xFFEE33, pantsColor:0x5599FF, hairColor:0x111111, char:'maenggu'}
  ],
  hangyodon: [
    {name:'한교동', bodyColor:0x56C2C2, bellyColor:0xAADDCC, accentColor:0x00AEEF, char:'hangyodon', lipColor:0xF9A7B0}
  ],
  doraemon: [
    {name:'도라에몽', bodyColor:0x2288EE, bellyColor:0xFFFFFF, accentColor:0xEE2222, char:'doraemon', bellColor:0xFFDD00}
  ],
};
let currentTheme = 'shinchan';
let PLUSHIE_TYPES = PLUSHIE_THEMES[currentTheme];

function createPlushie(type, position, rotation){
  const g = new THREE.Group();
  const t = type;
  const s = 0.35;

  // ── Helper: add cute face (eyes, highlights, blush, mouth) ──
  function addCuteFace(yHead, zFront, eyeSize, hasMouth){
    const eyeGeo = new THREE.SphereGeometry(s*eyeSize, 8, 8);
    const eyeMat = new THREE.MeshStandardMaterial({color:0x111111, roughness:0.3});
    [-1,1].forEach(side=>{
      const eye = new THREE.Mesh(eyeGeo, eyeMat);
      eye.position.set(side*s*0.28, yHead+s*0.08, zFront);
      g.add(eye);
    });
    const hlGeo = new THREE.SphereGeometry(s*0.04, 6, 6);
    const hlMat = new THREE.MeshStandardMaterial({color:0xffffff, emissive:0xffffff, emissiveIntensity:0.5});
    [-1,1].forEach(side=>{
      const hl = new THREE.Mesh(hlGeo, hlMat);
      hl.position.set(side*s*0.22, yHead+s*0.13, zFront+s*0.06);
      g.add(hl);
    });
    const blushGeo = new THREE.SphereGeometry(s*0.12, 8, 6);
    const blushMat = new THREE.MeshStandardMaterial({color:0xFF9999, transparent:true, opacity:0.3, roughness:1});
    [-1,1].forEach(side=>{
      const bl = new THREE.Mesh(blushGeo, blushMat);
      bl.position.set(side*s*0.48, yHead-s*0.05, zFront-s*0.05);
      g.add(bl);
    });
    if(hasMouth){
      const mGeo = new THREE.TorusGeometry(s*0.1, s*0.02, 6, 10, Math.PI);
      const mMat = new THREE.MeshStandardMaterial({color:0x222222, roughness:0.5});
      const m = new THREE.Mesh(mGeo, mMat);
      m.position.set(0, yHead-s*0.2, zFront);
      g.add(m);
    }
  }

  // ══════════════════════════════════════
  // THEME: 짱구 (Shin-chan characters)
  // ══════════════════════════════════════
  if(t.char === 'shiro'){
    // 흰둥이 - white dog
    const bodyGeo = new THREE.SphereGeometry(s*1.0, 16, 12);
    bodyGeo.scale(1.1, 0.9, 1.0);
    const bodyMat = new THREE.MeshStandardMaterial({color:0xFAFAFA, roughness:0.92});
    const body = new THREE.Mesh(bodyGeo, bodyMat); body.castShadow=true; g.add(body);
    const headGeo = new THREE.SphereGeometry(s*0.85, 16, 12);
    const headMat = new THREE.MeshStandardMaterial({color:0xFAFAFA, roughness:0.9});
    const head = new THREE.Mesh(headGeo, headMat); head.position.y=s*1.4; head.castShadow=true; g.add(head);
    const snoutGeo = new THREE.SphereGeometry(s*0.4, 10, 8);
    const snout = new THREE.Mesh(snoutGeo, new THREE.MeshStandardMaterial({color:0xFFF5EE, roughness:0.95}));
    snout.position.set(0, s*1.25, s*0.55); snout.scale.set(1,0.7,0.7); g.add(snout);
    addCuteFace(s*1.4, s*0.65, 0.08, false);
    const noseGeo = new THREE.SphereGeometry(s*0.12, 8, 6);
    const nose = new THREE.Mesh(noseGeo, new THREE.MeshStandardMaterial({color:0x111111, roughness:0.3}));
    nose.position.set(0, s*1.35, s*0.82); g.add(nose);
    const earGeo = new THREE.SphereGeometry(s*0.3, 10, 8); earGeo.scale(0.6,1.3,0.5);
    const earMat = new THREE.MeshStandardMaterial({color:0xF0F0F0, roughness:0.92});
    [-1,1].forEach(side=>{ const e=new THREE.Mesh(earGeo.clone(),earMat); e.position.set(side*s*0.6,s*1.6,-s*0.05); e.rotation.z=side*0.7; g.add(e); });
    const legGeo = new THREE.CapsuleGeometry(s*0.16, s*0.2, 6, 8);
    const legMat = new THREE.MeshStandardMaterial({color:0xFAFAFA, roughness:0.92});
    [[-1,1],[1,1],[-1,-1],[1,-1]].forEach(([sx,sz])=>{ const l=new THREE.Mesh(legGeo,legMat); l.position.set(sx*s*0.55,-s*0.8,sz*s*0.3); g.add(l); });
    const tail = new THREE.Mesh(new THREE.SphereGeometry(s*0.2,8,6), new THREE.MeshStandardMaterial({color:0xFAFAFA, roughness:0.92}));
    tail.position.set(0,s*0.2,-s*0.8); g.add(tail);

  } else if(t.char === 'shin' || t.char === 'himawari' || t.char === 'maenggu'){
    // ── 짱구 가족 & 친구들 (Shin-chan, Himawari, Bo-chan) ──
    const skinMat = new THREE.MeshStandardMaterial({color:t.skinColor, roughness:0.8});
    const shirtMat = new THREE.MeshStandardMaterial({color:t.shirtColor, roughness:0.9});
    const pantsMat = new THREE.MeshStandardMaterial({color:t.pantsColor, roughness:0.9});
    const hairMat = new THREE.MeshStandardMaterial({color:0x050505, roughness:0.9});
    const whiteMat = new THREE.MeshStandardMaterial({color:0xFFFFFF});
    const blackMat = new THREE.MeshStandardMaterial({color:0x111111});

    // Body
    const bodyGeo = new THREE.SphereGeometry(s*1.0, 16, 12); bodyGeo.scale(1, 1.15, 0.85);
    const body = new THREE.Mesh(bodyGeo, shirtMat); body.castShadow=true; g.add(body);
    
    if(t.char==='shin' || t.char==='maenggu'){
      const pantsGeo = new THREE.SphereGeometry(s*0.85, 12, 10); pantsGeo.scale(1.1, 0.6, 0.85);
      const pants = new THREE.Mesh(pantsGeo, pantsMat); pants.position.y=-s*0.85; g.add(pants);
    }

    // Head Group
    const headGroup = new THREE.Group();
    headGroup.position.y = s*1.6;
    g.add(headGroup);

    const headBase = new THREE.Mesh(new THREE.SphereGeometry(s*0.9, 16, 12), skinMat);
    headBase.scale.set(1.1, 0.95, 1.05); headGroup.add(headBase);
    const cheek = new THREE.Mesh(new THREE.SphereGeometry(s*0.65, 12, 12), skinMat);
    cheek.position.set(-s*0.55, -s*0.1, s*0.1); cheek.scale.set(1.25, 0.85, 1.1); headGroup.add(cheek);

    // Ears
    [-1,1].forEach(side=>{
      const ear = new THREE.Mesh(new THREE.SphereGeometry(s*0.18, 10, 8), skinMat);
      ear.position.set(side*s*0.95, 0, -s*0.1); ear.scale.set(0.6, 1, 0.8); headGroup.add(ear);
    });

    if(t.char==='shin'){
      // 짱구는 하단 Character-specific features 섹션에서 정교하게 모델링함
    } else if(t.char==='maenggu'){
      // 맹구 (Bo-chan): 길쭉한 타원형 얼굴
      headBase.scale.set(1.0, 1.35, 1.0);
      cheek.visible = false; // 맹구는 볼 돌출 없음
      
      // 맹구 눈 (작은 점눈)
      [-1,1].forEach(side=>{
        const eye = new THREE.Mesh(new THREE.SphereGeometry(s*0.08, 8, 8), blackMat);
        eye.position.set(side*s*0.25, s*0.2, s*0.85); headGroup.add(eye);
      });
      
      // 맹구 시그니처 콧물 (Runny nose)
      const snotGeo = new THREE.CapsuleGeometry(s*0.06, s*0.4, 4, 8);
      const snotMat = new THREE.MeshStandardMaterial({color:0xFFFFFF, transparent:true, opacity:0.85});
      const snot = new THREE.Mesh(snotGeo, snotMat);
      snot.position.set(0, -s*0.2, s*0.92); headGroup.add(snot);
      
      // 맹구 머리카락 (풍성하게 채우기)
      // 머리 전체를 감싸는 베이스 캡
      const hairBaseM = new THREE.Mesh(new THREE.SphereGeometry(s*0.95, 16, 12), hairMat);
      hairBaseM.position.set(0, s*0.45, -s*0.1); 
      hairBaseM.scale.set(1.05, 1.0, 1.0); headGroup.add(hairBaseM);

      // 원작 느낌의 가지런한 앞머리 (이마로 내려오는 삼각형 조각들)
      for(let i=0; i<5; i++){
        const spike = new THREE.Mesh(new THREE.ConeGeometry(s*0.15, s*0.35, 4), hairMat);
        const ang = (i / 4) * 1.2 - 0.6; // 이마 중앙 위주로 배치
        spike.position.set(Math.sin(ang)*s*0.6, s*0.95, Math.cos(ang)*s*0.75); 
        spike.rotation.x = Math.PI + 0.3; // 아래를 향하도록 회전
        spike.rotation.z = -ang * 0.5;
        headGroup.add(spike);
      }
    } else {
      // 짱아 머리 & 눈
      g.scale.set(0.85, 0.85, 0.85);
      const himaHairMat = new THREE.MeshStandardMaterial({color:t.hairColor, roughness:0.8});
      const hb = new THREE.Mesh(new THREE.SphereGeometry(s*0.85, 12, 12), himaHairMat);
      hb.position.set(0, s*0.25, -s*0.1); headGroup.add(hb);
      for(let i=0; i<4; i++) {
        const curl = new THREE.Mesh(new THREE.TorusGeometry(s*0.15, s*0.06, 8, 12), himaHairMat);
        curl.position.set((i-1.5)*s*0.35, s*0.8, s*0.25); curl.rotation.x = Math.PI/2; headGroup.add(curl);
      }
      const bib = new THREE.Mesh(new THREE.SphereGeometry(s*0.55, 12, 12), whiteMat);
      bib.position.set(0, -s*0.8, s*0.75); bib.scale.set(1.15, 1, 0.3); headGroup.add(bib);
      [-1,1].forEach(side=>{
        const ew = new THREE.Mesh(new THREE.SphereGeometry(s*0.3, 12, 12), whiteMat);
        ew.position.set(side*s*0.35, s*0.15, s*0.75); ew.scale.set(1, 1.1, 0.4); headGroup.add(ew);
        const p = new THREE.Mesh(new THREE.SphereGeometry(s*0.15, 8, 8), blackMat);
        p.position.set(side*s*0.35, s*0.12, s*0.9); headGroup.add(p);
      });
    }

    // Mouth
    if(t.char !== 'shin' && t.char !== 'maenggu'){
      const mouth = new THREE.Mesh(new THREE.TorusGeometry(s*0.12, s*0.025, 6, 10, Math.PI), blackMat);
      mouth.position.set(0, -s*0.2, s*0.85); mouth.rotation.x = Math.PI*0.1; headGroup.add(mouth);
    } else if(t.char === 'maenggu'){
      const mouth = new THREE.Mesh(new THREE.TorusGeometry(s*0.08, s*0.02, 6, 10, Math.PI), blackMat);
      mouth.position.set(0, -s*0.4, s*0.85); mouth.rotation.x = Math.PI; headGroup.add(mouth);
    }

    // Common Shoes/Socks
    [-1,1].forEach(side=>{
       const shoe = new THREE.Mesh(new THREE.CapsuleGeometry(s*0.15, s*0.22, 4, 8), new THREE.MeshStandardMaterial({color:(t.char==='shin'||t.char==='maenggu')?0xFFEE33:0xFFFFFF}));
       shoe.position.set(side*s*0.45, -s*1.45, s*0.1); shoe.rotation.x = Math.PI/2; g.add(shoe);
       const sock = new THREE.Mesh(new THREE.CylinderGeometry(s*0.16, s*0.16, s*0.15), whiteMat);
       sock.position.set(side*s*0.45, -s*1.25, 0); g.add(sock);
    });

    // Character-specific features
    if(t.char==='shin'){
      // 돌출된 볼 제거 및 얼굴 베이스 자체를 통통한 타원형으로 조정
      headBase.scale.set(1.25, 1.0, 1.05);

      // 밤톨 머리 (얼굴형에 맞춰 자연스럽게 배치)
      const hairBaseExtra = new THREE.Mesh(new THREE.SphereGeometry(s*1.05, 16, 12), hairMat); 
      hairBaseExtra.position.set(0, s*0.25, -s*0.1); 
      hairBaseExtra.scale.set(1.05, 0.9, 1.0); headGroup.add(hairBaseExtra);

      const hairFrontExtra = new THREE.Mesh(new THREE.SphereGeometry(s*1.05, 16, 12, 0, Math.PI*2, 0, Math.PI*0.35), hairMat);
      hairFrontExtra.position.set(0, s*0.2, s*0.1); 
      hairFrontExtra.rotation.x = -0.45; headGroup.add(hairFrontExtra);

      // BIG Expressive Eyes (중앙 정렬 최적화)
      const eyeWGeo2 = new THREE.SphereGeometry(s*0.35, 12, 12);
      const eyeWMat2 = new THREE.MeshStandardMaterial({color:0xFFFFFF});
      const pupGeo2 = new THREE.SphereGeometry(s*0.16, 8, 8);
      const pupMat2 = new THREE.MeshStandardMaterial({color:0x111111});
      [-1,1].forEach(side=>{
        const ew=new THREE.Mesh(eyeWGeo2, eyeWMat2); 
        ew.position.set(side*s*0.35, s*0.1, s*0.9); 
        ew.scale.set(1, 1.15, 0.4); headGroup.add(ew);
        
        const p=new THREE.Mesh(pupGeo2, pupMat2); 
        p.position.set(side*s*0.35, s*0.05, s*1.05); 
        headGroup.add(p);
      });

      // VERY Thick Signature Eyebrows
      const browGeo=new THREE.CapsuleGeometry(s*0.18, s*0.45, 6, 8); 
      const browMat=new THREE.MeshStandardMaterial({color:0x050505});
      [-1,1].forEach(side=>{ 
        const b=new THREE.Mesh(browGeo, browMat); 
        b.position.set(side*s*0.4, s*0.6, s*0.9); 
        b.rotation.z=side*(-0.1) + Math.PI/2; headGroup.add(b); 
      });

      // Mouth (정중앙에 배치하여 깔끔하게 변경)
      const mouthGeo = new THREE.SphereGeometry(s*0.15, 12, 10);
      const mouthMat = new THREE.MeshStandardMaterial({color:0x442222}); 
      const mouth = new THREE.Mesh(mouthGeo, mouthMat);
      mouth.position.set(0, -s*0.3, s*1.05); 
      mouth.scale.set(0.7, 1.2, 0.4); 
      headGroup.add(mouth);

      // Ears
      const earGeo = new THREE.SphereGeometry(s*0.22, 10, 8);
      [-1,1].forEach(side=>{
        const ear = new THREE.Mesh(earGeo, skinMat);
        ear.position.set(side*s*1.2, -s*0.05, -s*0.1);
        ear.scale.set(0.6, 1, 0.8); headGroup.add(ear);
      });
    } else if(t.char==='himawari'){
      // 짱아 (Himawari)
      const skinMat = new THREE.MeshStandardMaterial({color:t.skinColor, roughness:0.8});
      const shirtMat = new THREE.MeshStandardMaterial({color:t.shirtColor, roughness:0.9});
      const hairMat = new THREE.MeshStandardMaterial({color:t.hairColor, roughness:0.8});
      const whiteMat = new THREE.MeshStandardMaterial({color:0xFFFFFF});
      const blackMat = new THREE.MeshStandardMaterial({color:0x111111});

      g.scale.set(0.85, 0.85, 0.85);
      const bodyGeo = new THREE.SphereGeometry(s*1.1, 16, 12);
      const body = new THREE.Mesh(bodyGeo, shirtMat); body.castShadow=true; g.add(body);
      
      const headGroupHima = new THREE.Group();
      headGroupHima.position.y = s*1.6; g.add(headGroupHima);
      
      const headBaseHima = new THREE.Mesh(new THREE.SphereGeometry(s*0.9, 16, 12), skinMat);
      headBaseHima.scale.set(1.1, 0.95, 1.05); headGroupHima.add(headBaseHima);
      const cheekHima = new THREE.Mesh(new THREE.SphereGeometry(s*0.6, 12, 12), skinMat);
      cheekHima.position.set(-s*0.5, -s*0.05, s*0.1); cheekHima.scale.set(1.25, 0.85, 1.1); headGroupHima.add(cheekHima);

      // Ears
      const earGeoHima = new THREE.SphereGeometry(s*0.18, 10, 8);
      [-1,1].forEach(side=>{
        const ear = new THREE.Mesh(earGeoHima, skinMat);
        ear.position.set(side*s*0.9, 0, -s*0.1); ear.scale.set(0.6, 1, 0.8); headGroupHima.add(ear);
      });

      // Bib
      const bib = new THREE.Mesh(new THREE.SphereGeometry(s*0.55, 12, 12), whiteMat);
      bib.position.set(0, -s*0.8, s*0.75); bib.scale.set(1.15, 1, 0.3); headGroup.add(bib);

      // Curly Hair
      const hb = new THREE.Mesh(new THREE.SphereGeometry(s*0.85, 12, 12), hairMat);
      hb.position.set(0, s*0.25, -s*0.1); headGroup.add(hb);
      for(let i=0; i<4; i++) {
        const curl = new THREE.Mesh(new THREE.TorusGeometry(s*0.15, s*0.06, 8, 12), hairMat);
        curl.position.set((i-1.5)*s*0.35, s*0.8, s*0.25); curl.rotation.x = Math.PI/2; headGroup.add(curl);
      }

      // Eyes
      const eyeWGeo = new THREE.SphereGeometry(s*0.3, 12, 12);
      [-1,1].forEach(side=>{
        const ew = new THREE.Mesh(eyeWGeo, whiteMat);
        ew.position.set(side*s*0.35, s*0.15, s*0.75); ew.scale.set(1, 1.1, 0.4); headGroup.add(ew);
        const p = new THREE.Mesh(new THREE.SphereGeometry(s*0.15, 8, 8), blackMat);
        p.position.set(side*s*0.35, s*0.12, s*0.9); headGroup.add(p);
      });
      
      // Common Mouth
      const mouth = new THREE.Mesh(new THREE.TorusGeometry(s*0.12, s*0.025, 6, 10, Math.PI), blackMat);
      mouth.position.set(0, -s*0.2, s*0.85); mouth.rotation.x = Math.PI*0.1; headGroup.add(mouth);

      // Shoes
      [-1,1].forEach(side=>{
         const shoe = new THREE.Mesh(new THREE.CapsuleGeometry(s*0.15, s*0.22, 4, 8), whiteMat);
         shoe.position.set(side*s*0.45, -s*1.45, s*0.1); shoe.rotation.x = Math.PI/2; g.add(shoe);
      });
    }

  // ══════════════════════════════════════
  // THEME: 시나모롤 (Sanrio cute animals)
  // ══════════════════════════════════════
  } else if(['cinna','mocha','cappuccino','espresso','milk','chiffon'].includes(t.char)){
    // Round fluffy body
    const bodyGeo = new THREE.SphereGeometry(s*1.1, 16, 12);
    bodyGeo.scale(1, 0.9, 0.9); // 몸통을 살짝 낮게
    const bodyMat = new THREE.MeshStandardMaterial({color:t.bodyColor, roughness:0.92});
    const body = new THREE.Mesh(bodyGeo, bodyMat); body.castShadow=true; g.add(body);
    
    // Belly
    const bellyGeo = new THREE.SphereGeometry(s*0.7, 12, 10);
    const belly = new THREE.Mesh(bellyGeo, new THREE.MeshStandardMaterial({color:t.bellyColor, roughness:0.95}));
    belly.position.set(0, -s*0.1, s*0.5); belly.scale.set(1,1,0.5); g.add(belly);
    
    // Wide and flat head (원작의 납작한 호빵 모양)
    const headGroupC = new THREE.Group();
    headGroupC.position.y = s*1.5;
    g.add(headGroupC);

    const headBaseC = new THREE.Mesh(new THREE.SphereGeometry(s*1.0, 16, 12), bodyMat);
    headBaseC.scale.set(1.3, 0.85, 1.0); headBaseC.castShadow=true; headGroupC.add(headBaseC);

    // Cute face features
    // Blue Oval Eyes (시나모롤의 상징!)
    const eyeMatC = new THREE.MeshStandardMaterial({color:t.char==='cinna'?0x55AAFF:0x442211, roughness:0.3});
    [-1,1].forEach(side=>{
      const eye = new THREE.Mesh(new THREE.SphereGeometry(s*0.15, 12, 12), eyeMatC);
      eye.position.set(side*s*0.45, s*0.05, s*0.82);
      eye.scale.set(1, 1.2, 0.4); // 세로형 타원
      headGroupC.add(eye);
    });

    // Pink Blush
    const blushMatC = new THREE.MeshStandardMaterial({color:0xFFAACC, transparent:true, opacity:0.5});
    [-1,1].forEach(side=>{
      const blush = new THREE.Mesh(new THREE.SphereGeometry(s*0.2, 12, 8), blushMatC);
      blush.position.set(side*s*0.7, -s*0.1, s*0.75);
      blush.scale.set(1, 0.6, 0.3);
      headGroupC.add(blush);
    });

    // Mouth ('w' shape logic)
    const mouthGroup = new THREE.Group();
    mouthGroup.position.set(0, -s*0.1, s*0.9);
    const mMatC = new THREE.MeshStandardMaterial({color:0x885533, roughness:0.5});
    [-1,1].forEach(side=>{
      const lip = new THREE.Mesh(new THREE.TorusGeometry(s*0.08, s*0.02, 8, 12, Math.PI), mMatC);
      lip.position.x = side*s*0.075;
      lip.rotation.x = Math.PI;
      mouthGroup.add(lip);
    });
    headGroupC.add(mouthGroup);

    // Signature Floppy Ears (원작의 크고 납작한 귀!)
    const earGeoC = new THREE.CapsuleGeometry(s*0.2, s*1.2, 8, 12);
    [-1,1].forEach(side=>{
      const ear = new THREE.Mesh(earGeoC, bodyMat);
      ear.position.set(side*s*0.8, s*0.1, 0);
      ear.rotation.z = side * (Math.PI * 0.45); // 옆으로 쭉 뻗게
      ear.rotation.y = -side * 0.2;
      ear.scale.set(1.5, 1, 0.5); // 귀를 납작하게
      headGroupC.add(ear);
    });

    // Short limbs
    const armMat = new THREE.MeshStandardMaterial({color:t.bodyColor, roughness:0.92});
    [-1,1].forEach(side=>{ 
      const a=new THREE.Mesh(new THREE.CapsuleGeometry(s*0.18, s*0.25, 6, 8), armMat); 
      a.position.set(side*s*0.8, s*0.1, s*0.3); 
      a.rotation.z=side*0.4; a.rotation.x=0.5; g.add(a); 
    });
    [-1,1].forEach(side=>{ 
      const l=new THREE.Mesh(new THREE.CapsuleGeometry(s*0.2, s*0.15, 6, 8), armMat); 
      l.position.set(side*s*0.45, -s*0.9, s*0.1); g.add(l); 
    });

    // Cinnamon Roll Tail (돌돌 말린 꼬리)
    const tailGeoC = new THREE.TorusGeometry(s*0.25, s*0.1, 8, 16, Math.PI*1.8);
    const tail = new THREE.Mesh(tailGeoC, bodyMat);
    tail.position.set(0, -s*0.2, -s*0.8);
    tail.rotation.y = Math.PI/2;
    g.add(tail);

    // Ribbon (Other characters)
    if(t.char!=='cinna' && t.ribbonColor){
      const bowMat = new THREE.MeshStandardMaterial({color:t.ribbonColor, roughness:0.5});
      const bowGeo = new THREE.SphereGeometry(s*0.12, 6, 6); bowGeo.scale(1.6,0.9,0.7);
      [-1,1].forEach(side=>{ const b=new THREE.Mesh(bowGeo.clone(),bowMat); b.position.set(side*s*0.15,s*0.8,s*0.3); headGroupC.add(b); });
      const knot = new THREE.Mesh(new THREE.SphereGeometry(s*0.06,6,6), bowMat);
      knot.position.set(0,s*0.8,s*0.35); headGroupC.add(knot);
    }

  // ══════════════════════════════════════
  // THEME: 공룡 (Dinosaurs)
  // ══════════════════════════════════════
  } else if(['trex','trice','brachi','stego','ptera','baby'].includes(t.char)){
    // Chubby body
    const bodyGeo = new THREE.SphereGeometry(s*1.15, 16, 12);
    bodyGeo.scale(1, 1.1, 0.95);
    const bodyMat = new THREE.MeshStandardMaterial({color:t.bodyColor, roughness:0.88});
    const body = new THREE.Mesh(bodyGeo, bodyMat); body.castShadow=true; g.add(body);
    // Belly
    const belly = new THREE.Mesh(new THREE.SphereGeometry(s*0.75,12,10), new THREE.MeshStandardMaterial({color:t.bellyColor, roughness:0.92}));
    belly.position.set(0, -s*0.1, s*0.5); belly.scale.set(1,1.1,0.5); g.add(belly);
    // Head
    const headGeo = new THREE.SphereGeometry(s*0.8, 16, 12);
    const headMat = new THREE.MeshStandardMaterial({color:t.bodyColor, roughness:0.88});
    const head = new THREE.Mesh(headGeo, headMat); head.position.y=s*1.5; head.castShadow=true; g.add(head);
    // Snout
    const snout = new THREE.Mesh(new THREE.SphereGeometry(s*0.4,10,8), headMat);
    snout.position.set(0, s*1.35, s*0.55); snout.scale.set(1,0.7,0.8); g.add(snout);
    // Cute face
    addCuteFace(s*1.45, s*0.6, 0.1, true);
    // Nostrils
    const nostrilMat = new THREE.MeshStandardMaterial({color:t.accentColor, roughness:0.7});
    [-1,1].forEach(side=>{ const n=new THREE.Mesh(new THREE.SphereGeometry(s*0.05,6,6),nostrilMat); n.position.set(side*s*0.12,s*1.38,s*0.88); g.add(n); });
    // Short arms
    const armMat = new THREE.MeshStandardMaterial({color:t.bodyColor, roughness:0.88});
    [-1,1].forEach(side=>{ const a=new THREE.Mesh(new THREE.CapsuleGeometry(s*0.15,s*0.2,6,8),armMat); a.position.set(side*s*1.0,s*0.15,0); a.rotation.z=side*0.6; g.add(a); });
    // Chunky legs
    [-1,1].forEach(side=>{ const l=new THREE.Mesh(new THREE.CapsuleGeometry(s*0.2,s*0.2,6,8),armMat); l.position.set(side*s*0.45,-s*1.0,s*0.1); g.add(l); });
    // Tail
    const tailGeo = new THREE.CapsuleGeometry(s*0.15, s*0.5, 6, 8);
    const tail = new THREE.Mesh(tailGeo, armMat);
    tail.position.set(0, -s*0.2, -s*1.0); tail.rotation.x=0.5; g.add(tail);

    // Back spines (all dinos get some)
    const spineColor = t.spineColor || t.accentColor;
    const spineMat = new THREE.MeshStandardMaterial({color:spineColor, roughness:0.85});

    if(t.char==='trex'){
      // Small spines
      for(let i=0;i<5;i++){ const sp=new THREE.Mesh(new THREE.ConeGeometry(s*0.08,s*0.2,5),spineMat); sp.position.set(0,s*(1.8-i*0.5),-s*0.5-i*s*0.08); sp.rotation.x=-0.3; g.add(sp); }
      // Bigger jaw
      const jaw = new THREE.Mesh(new THREE.SphereGeometry(s*0.3,8,6), headMat);
      jaw.position.set(0, s*1.15, s*0.65); jaw.scale.set(1,0.5,0.8); g.add(jaw);
    } else if(t.char==='trice'){
      // Horn
      const hornMat = new THREE.MeshStandardMaterial({color:0xEEDDAA, roughness:0.7});
      const horn = new THREE.Mesh(new THREE.ConeGeometry(s*0.08,s*0.35,6), hornMat);
      horn.position.set(0, s*2.0, s*0.35); horn.rotation.x=-0.3; g.add(horn);
      // Side horns
      [-1,1].forEach(side=>{ const h=new THREE.Mesh(new THREE.ConeGeometry(s*0.06,s*0.25,5),hornMat); h.position.set(side*s*0.4,s*1.9,s*0.2); h.rotation.z=side*0.4; h.rotation.x=-0.2; g.add(h); });
      // Frill
      const frill = new THREE.Mesh(new THREE.SphereGeometry(s*0.7,12,8), new THREE.MeshStandardMaterial({color:t.accentColor, roughness:0.85}));
      frill.position.set(0, s*1.9, -s*0.3); frill.scale.set(1.2,0.8,0.3); g.add(frill);
    } else if(t.char==='brachi'){
      // Long neck extension
      const neck = new THREE.Mesh(new THREE.CapsuleGeometry(s*0.18,s*0.5,6,8), armMat);
      neck.position.set(0, s*2.1, s*0.1); g.add(neck);
      // Smaller head on top
      const sHead = new THREE.Mesh(new THREE.SphereGeometry(s*0.45,12,8), headMat);
      sHead.position.set(0, s*2.7, s*0.2); g.add(sHead);
      for(let i=0;i<4;i++){ const sp=new THREE.Mesh(new THREE.ConeGeometry(s*0.06,s*0.15,5),spineMat); sp.position.set(0,s*(2.5-i*0.6),-s*0.35); g.add(sp); }
    } else if(t.char==='stego'){
      // Big back plates
      for(let i=0;i<6;i++){ const plate=new THREE.Mesh(new THREE.ConeGeometry(s*0.12,s*0.3,4),spineMat); plate.position.set(0,s*(1.6-i*0.35),-s*0.55); plate.rotation.x=-0.6; g.add(plate); }
      // Tail spikes
      [-1,1].forEach(side=>{ const sp=new THREE.Mesh(new THREE.ConeGeometry(s*0.05,s*0.2,4),spineMat); sp.position.set(side*s*0.15,-s*0.5,-s*1.2); sp.rotation.x=0.8; g.add(sp); });
    } else if(t.char==='ptera'){
      // Wings!
      const wingMat = new THREE.MeshStandardMaterial({color:t.bodyColor, roughness:0.85, side:THREE.DoubleSide});
      [-1,1].forEach(side=>{
        const wingGeo = new THREE.PlaneGeometry(s*1.5, s*0.8, 1, 1);
        const wing = new THREE.Mesh(wingGeo, wingMat);
        wing.position.set(side*s*1.2, s*0.4, -s*0.1);
        wing.rotation.z = side*(-0.3);
        wing.rotation.y = side*0.2;
        g.add(wing);
      });
      // Head crest
      const crest = new THREE.Mesh(new THREE.ConeGeometry(s*0.1,s*0.5,5), spineMat);
      crest.position.set(0, s*2.1, -s*0.2); crest.rotation.x=-0.5; g.add(crest);
    } else if(t.char==='baby'){
      // Egg shell piece on head
      const shellMat = new THREE.MeshStandardMaterial({color:0xFFFFF0, roughness:0.8});
      const shell = new THREE.Mesh(new THREE.SphereGeometry(s*0.55,10,6,0,Math.PI*2,0,Math.PI*0.5), shellMat);
      shell.position.set(0, s*1.85, 0); shell.rotation.z=0.2; g.add(shell);
      for(let i=0;i<3;i++){ const sp=new THREE.Mesh(new THREE.ConeGeometry(s*0.06,s*0.12,5),spineMat); sp.position.set(0,s*(1.6-i*0.5),-s*0.45); g.add(sp); }
    }

  // ══════════════════════════════════════
  // THEME: 곰돌이 (Bears)
  // ══════════════════════════════════════
  } else if(['honey','polar','brown','panda','strawberry','sky'].includes(t.char)){
    // Round body
    const bodyGeo = new THREE.SphereGeometry(s*1.1, 16, 12);
    bodyGeo.scale(1, 1.2, 0.9);
    const bodyMat = new THREE.MeshStandardMaterial({color:t.bodyColor, roughness:0.9});
    const body = new THREE.Mesh(bodyGeo, bodyMat); body.castShadow=true; g.add(body);
    // Belly
    const belly = new THREE.Mesh(new THREE.SphereGeometry(s*0.7,12,10), new THREE.MeshStandardMaterial({color:t.bellyColor, roughness:0.95}));
    belly.position.set(0, -s*0.1, s*0.5); belly.scale.set(1,1.1,0.6); g.add(belly);
    // Head
    const head = new THREE.Mesh(new THREE.SphereGeometry(s*0.85,16,12), new THREE.MeshStandardMaterial({color:t.bodyColor, roughness:0.88}));
    head.position.y=s*1.6; head.castShadow=true; g.add(head);
    // Muzzle
    const muzzle = new THREE.Mesh(new THREE.SphereGeometry(s*0.45,12,8), new THREE.MeshStandardMaterial({color:t.bellyColor, roughness:0.95}));
    muzzle.position.set(0, s*1.4, s*0.55); muzzle.scale.set(1,0.8,0.7); g.add(muzzle);
    // Cute face
    addCuteFace(s*1.55, s*0.65, 0.12, true);
    // Nose
    const nose = new THREE.Mesh(new THREE.SphereGeometry(s*0.1,8,6), new THREE.MeshStandardMaterial({color:0x222222, roughness:0.4}));
    nose.position.set(0, s*1.5, s*0.82); g.add(nose);
    // Round ears
    const earGeo = new THREE.SphereGeometry(s*0.35, 10, 8);
    const earMat = new THREE.MeshStandardMaterial({color:t.bodyColor, roughness:0.9});
    const earInGeo = new THREE.SphereGeometry(s*0.2, 8, 6);
    const earInMat = new THREE.MeshStandardMaterial({color:t.accentColor, roughness:0.95});
    [-1,1].forEach(side=>{
      const ear=new THREE.Mesh(earGeo,earMat); ear.position.set(side*s*0.6,s*2.25,0); ear.castShadow=true; g.add(ear);
      const ein=new THREE.Mesh(earInGeo,earInMat); ein.position.set(side*s*0.6,s*2.25,s*0.15); g.add(ein);
    });
    // Arms
    const armGeo = new THREE.CapsuleGeometry(s*0.18, s*0.35, 6, 8);
    const armMat = new THREE.MeshStandardMaterial({color:t.bodyColor, roughness:0.9});
    [-1,1].forEach(side=>{ const a=new THREE.Mesh(armGeo,armMat); a.position.set(side*s*1.1,s*0.3,0); a.rotation.z=side*0.4; a.castShadow=true; g.add(a);
      const pad=new THREE.Mesh(new THREE.SphereGeometry(s*0.12,6,6),new THREE.MeshStandardMaterial({color:t.bellyColor,roughness:0.95})); pad.position.set(side*s*1.25,s*0.05,s*0.1); g.add(pad); });
    // Legs
    const legGeo = new THREE.SphereGeometry(s*0.3, 10, 8); legGeo.scale(1,0.7,1.1);
    [-1,1].forEach(side=>{ const l=new THREE.Mesh(legGeo.clone(),armMat); l.position.set(side*s*0.5,-s*0.95,s*0.15); l.castShadow=true; g.add(l);
      const fp=new THREE.Mesh(new THREE.SphereGeometry(s*0.18,8,6),new THREE.MeshStandardMaterial({color:t.bellyColor,roughness:0.95})); fp.position.set(side*s*0.5,-s*0.95,s*0.35); fp.scale.set(1,0.7,0.6); g.add(fp); });
    // Ribbon bow
    if(t.ribbonColor){
      const bowMat=new THREE.MeshStandardMaterial({color:t.ribbonColor, roughness:0.6});
      const bowGeo=new THREE.SphereGeometry(s*0.1,6,6); bowGeo.scale(1.5,0.8,0.6);
      [-1,1].forEach(side=>{ const b=new THREE.Mesh(bowGeo.clone(),bowMat); b.position.set(side*s*0.18,s*1.05,s*0.35); g.add(b); });
      const ribbon=new THREE.Mesh(new THREE.TorusGeometry(s*0.12,s*0.04,6,12),bowMat); ribbon.position.set(0,s*1.05,s*0.3); g.add(ribbon);
    }
    // Panda patches
    if(t.char==='panda'){
      const patchMat=new THREE.MeshStandardMaterial({color:0x222222, roughness:0.9});
      [-1,1].forEach(side=>{ const p=new THREE.Mesh(new THREE.SphereGeometry(s*0.25,8,8),patchMat); p.position.set(side*s*0.3,s*1.75,s*0.45); p.scale.set(1,0.8,0.5); g.add(p); });
    }

  // ══════════════════════════════════════
  // THEME: 한교동 (Hangyodon & Friends)
  // ══════════════════════════════════════
  } else {
    if(t.char==='hangyodon'){
      // 한교동 — (민트색 반어인)
      const bodyGeo = new THREE.SphereGeometry(s*1.1,16,12); bodyGeo.scale(1,1.15,0.9);
      const bodyMat = new THREE.MeshStandardMaterial({color:t.bodyColor, roughness:0.88});
      const body = new THREE.Mesh(bodyGeo, bodyMat); body.castShadow=true; g.add(body);

      const belly = new THREE.Mesh(new THREE.SphereGeometry(s*0.7,12,10), new THREE.MeshStandardMaterial({color:t.bellyColor, roughness:0.92}));
      belly.position.set(0,-s*0.1,s*0.5); belly.scale.set(1,1.1,0.5); g.add(belly);

      // Scales (U marks on chest)
      const scaleMat = new THREE.MeshStandardMaterial({color:0x44AAAA, roughness:0.5});
      [[-0.2,0.1,0.9],[0.2,0.1,0.9],[0,-0.2,0.9]].forEach(p=>{
         const sc = new THREE.Mesh(new THREE.TorusGeometry(s*0.1, s*0.02, 8, 8, Math.PI), scaleMat);
         sc.position.set(p[0]*s, p[1]*s, p[2]*s); sc.rotation.x = -Math.PI/2; g.add(sc);
      });

      // Big Head
      const head = new THREE.Mesh(new THREE.SphereGeometry(s*1.1,16,12), bodyMat);
      head.position.y=s*1.6; head.castShadow=true; g.add(head);

      // BIG white eyes (round)
      const eyeWGeo=new THREE.SphereGeometry(s*0.35,12,12);
      const eyeWMat=new THREE.MeshStandardMaterial({color:0xFFFFFF, roughness:0.2});
      const pupGeo2=new THREE.SphereGeometry(s*0.08,8,8);
      const pupMat2=new THREE.MeshStandardMaterial({color:0x111111, roughness:0.1});
      [-1,1].forEach(side=>{
        const ew=new THREE.Mesh(eyeWGeo,eyeWMat); ew.position.set(side*s*0.4,s*1.8,s*0.95); ew.scale.set(1,1.1,0.5); g.add(ew);
        const pu=new THREE.Mesh(pupGeo2,pupMat2); pu.position.set(side*s*0.4,s*1.8,s*1.15); g.add(pu);
      });

      // BIG thick pink lips (one piece wide)
      const lipMat=new THREE.MeshStandardMaterial({color:t.lipColor, roughness:0.6});
      const lipMain=new THREE.Mesh(new THREE.CapsuleGeometry(s*0.35, s*0.6, 12, 12), lipMat);
      lipMain.position.set(0,s*1.2,s*1.0); lipMain.rotation.z=Math.PI/2; lipMain.scale.set(0.6,1.4,0.6); g.add(lipMain);

      // Side fins (Ears)
      const sideFinGeo = new THREE.SphereGeometry(s*0.25, 12, 8); sideFinGeo.scale(1,1.5,0.4);
      const sideFinMat = new THREE.MeshStandardMaterial({color:t.accentColor, roughness:0.7});
      [-1,1].forEach(side=>{
        const sf = new THREE.Mesh(sideFinGeo, sideFinMat);
        sf.position.set(side*s*1.15, s*1.6, s*0.3); sf.rotation.z = side*0.2; g.add(sf);
      });

      // Dorsal fin (3-bump wave)
      const dorsalBase = new THREE.Group();
      [0, 0.25, 0.5].forEach((z, i)=>{
          const bump = new THREE.Mesh(new THREE.SphereGeometry(s*(0.25-i*0.05), 10, 8), sideFinMat);
          bump.position.set(0, s*(0.4-i*0.1), -z*s); bump.scale.y=1.5;
          dorsalBase.add(bump);
      });
      dorsalBase.position.set(0, s*2.5, s*0.2); dorsalBase.rotation.x = -0.2; g.add(dorsalBase);

      // Stubby fin-arms
      [-1,1].forEach(side=>{
        const fin=new THREE.Mesh(new THREE.CapsuleGeometry(s*0.15,s*0.3,6,8), bodyMat);
        fin.position.set(side*s*1.1,s*0.2,0); fin.rotation.z=side*0.8; g.add(fin);
      });

      // Short legs
      [-1,1].forEach(side=>{
        const l=new THREE.Mesh(new THREE.CapsuleGeometry(s*0.18,s*0.15,6,8), bodyMat);
        l.position.set(side*s*0.4,-s*1.0,s*0.1); g.add(l);
      });

      // Small Tail
      const tail = new THREE.Mesh(new THREE.SphereGeometry(s*0.3, 8, 8), sideFinMat);
      tail.position.set(0, -s*0.3, -s*0.8); tail.scale.set(0.5,1,1.2); g.add(tail);
    }

    if(t.char==='doraemon'){
      // 도라에몽 — (파란색 고양이형 로봇)
      const bodyGeo = new THREE.SphereGeometry(s*1.0, 16, 12); bodyGeo.scale(1, 1.1, 0.9);
      const bodyMat = new THREE.MeshStandardMaterial({color:t.bodyColor, roughness:0.7});
      const whiteMat = new THREE.MeshStandardMaterial({color:0xFFFFFF, roughness:0.6});
      
      const body = new THREE.Mesh(bodyGeo, bodyMat); body.castShadow=true; g.add(body);
      
      // White Belly
      const belly = new THREE.Mesh(new THREE.SphereGeometry(s*0.82, 12, 10), whiteMat);
      belly.position.set(0, -s*0.1, s*0.42); belly.scale.set(1, 1, 0.5); g.add(belly);
      
      // Pocket line
      const pocket = new THREE.Mesh(new THREE.TorusGeometry(s*0.38, s*0.02, 8, 12, Math.PI), new THREE.MeshStandardMaterial({color:0x888888}));
      pocket.position.set(0, 0, s*0.95); pocket.rotation.x = -0.1; g.add(pocket);

      // Big Head
      const head = new THREE.Mesh(new THREE.SphereGeometry(s*1.25, 16, 12), bodyMat);
      head.position.y = s*1.7; head.castShadow=true; g.add(head);

      // Large White Face Mask (앞으로 확실히 당겨서 파란색과 분리)
      const faceMask = new THREE.Mesh(new THREE.SphereGeometry(s*1.15, 16, 12), whiteMat);
      faceMask.position.set(0, s*1.65, s*0.35); faceMask.scale.set(1.05, 1.0, 0.85); g.add(faceMask);

      // BIG Oval Eyes
      const eyeGeo = new THREE.SphereGeometry(s*0.35, 12, 12); eyeGeo.scale(0.85, 1.1, 0.4);
      [-1,1].forEach(side=>{
        const eye = new THREE.Mesh(eyeGeo, whiteMat);
        eye.position.set(side*s*0.3, s*2.3, s*1.2); g.add(eye);
        const pup = new THREE.Mesh(new THREE.SphereGeometry(s*0.06, 8, 8), new THREE.MeshStandardMaterial({color:0x111111}));
        pup.position.set(side*s*0.22, s*2.3, s*1.6); g.add(pup);
      });

      // Red Nose
      const nose = new THREE.Mesh(new THREE.SphereGeometry(s*0.13, 10, 10), new THREE.MeshStandardMaterial({color:t.accentColor, roughness:0.4}));
      nose.position.set(0, s*2.0, s*1.6); g.add(nose);

      // Nose-to-Mouth Line
      const whiskerMat = new THREE.MeshStandardMaterial({color:0x333333});
      const line = new THREE.Mesh(new THREE.BoxGeometry(s*0.02, s*0.45, s*0.02), whiskerMat);
      line.position.set(0, s*1.7, s*1.55); g.add(line);

      // Smiling Mouth (반대로 뒤집힌 모양 수정 및 웃는 모양으로)
      const mouthGeo = new THREE.SphereGeometry(s*0.5, 12, 10, 0, Math.PI*2, 0, Math.PI*0.5);
      const mouth = new THREE.Mesh(mouthGeo, new THREE.MeshStandardMaterial({color:0xAA3322}));
      mouth.position.set(0, s*1.6, s*1.45); 
      mouth.rotation.x = Math.PI * 0.5; // 방향 회전
      mouth.scale.set(1, 0.7, 0.4); g.add(mouth);

      // Whiskers (앞으로 확실히 노출)
      [-1,1].forEach(side=>{
        [0.12, 0, -0.12].forEach((yOff, i)=>{
           const w = new THREE.Mesh(new THREE.BoxGeometry(s*0.6, s*0.02, s*0.02), whiskerMat);
           w.position.set(side*s*0.8, s*1.8 + yOff*s, s*1.5);
           w.rotation.z = side * (0.25 - i*0.25); g.add(w);
        });
      });

      // Collar & Bell
      const collar = new THREE.Mesh(new THREE.TorusGeometry(s*0.95, s*0.08, 8, 20), new THREE.MeshStandardMaterial({color:t.accentColor}));
      collar.position.y = s*0.85; collar.rotation.x = Math.PI/2; g.add(collar);
      const bell = new THREE.Mesh(new THREE.SphereGeometry(s*0.22, 12, 10), new THREE.MeshStandardMaterial({color:t.bellColor, metalness:0.7, roughness:0.2}));
      bell.position.set(0, s*0.7, s*1.05); g.add(bell);

      // Arms & Hands
      [-1,1].forEach(side=>{
        const arm = new THREE.Mesh(new THREE.CapsuleGeometry(s*0.22, s*0.35, 6, 8), bodyMat);
        arm.position.set(side*s*1.1, s*0.2, 0); arm.rotation.z = side*0.7; g.add(arm);
        const hand = new THREE.Mesh(new THREE.SphereGeometry(s*0.3, 12, 10), whiteMat);
        hand.position.set(side*s*1.38, -s*0.15, 0.12); g.add(hand);
      });

      // Legs & Feet
      [-1,1].forEach(side=>{
        const leg = new THREE.Mesh(new THREE.CapsuleGeometry(s*0.28, s*0.15, 6, 8), bodyMat);
        leg.position.set(side*s*0.45, -s*0.95, 0); g.add(leg);
        const foot = new THREE.Mesh(new THREE.SphereGeometry(s*0.38, 12, 10), whiteMat);
        foot.position.set(side*s*0.48, -s*1.4, 0.15); foot.scale.set(1, 0.6, 1.2); g.add(foot);
      });
    }

  }
  
  // Apply position and random rotation
  g.position.copy(position);
  g.rotation.set(rotation.x, rotation.y, rotation.z);

  // Bounding for grab detection
  g.userData = {type: t.name, char: t.char, theme: currentTheme, grabbed: false, radius: s*1.2};

  return g;
}

// Place plushies — packed full like a real machine
function spawnPlushies(){
  plushieGroup.clear();
  plushies.length = 0;

  // Play area bounds for spawning (full width)
  const playMinX = -BOX_W/2 + 0.3;
  const playMaxX = BOX_W/2 - 0.3;
  const playCX = 0;
  const playSpanX = playMaxX - playMinX;

  // Helper: check if position overlaps the hole area
  function isInHole(x, z){
    const margin = 0.15;
    return Math.abs(x - HOLE_POS.x) < (holeW/2 + wallT + margin)
        && Math.abs(z - HOLE_POS.z) < (holeD/2 + wallT + margin);
  }
  // Helper: clamp position inside box walls (0.45 margin for plushie radius)
  function clampInBox(x, z){
    const mx = BOX_W/2 - 0.45;
    const mz = BOX_D/2 - 0.45;
    return { x: Math.max(-mx, Math.min(mx, x)), z: Math.max(-mz, Math.min(mz, z)) };
  }

  // Layer 1: bottom layer, tightly packed
  const layer1Count = 18;
  for(let i = 0; i < layer1Count; i++){
    const typeIdx = i % PLUSHIE_TYPES.length;
    const type = PLUSHIE_TYPES[typeIdx];
    const col = i % 6, row = Math.floor(i / 6);
    let x = playMinX + 0.15 + col * ((playSpanX-0.3)/5) + (Math.random()-0.5)*0.2;
    let z = -BOX_D*0.35 + row * (BOX_D*0.35) + (Math.random()-0.5)*0.2;
    // If overlapping hole, shift away
    if(isInHole(x, z)){
      x = playMinX + Math.random() * playSpanX * 0.5;
      z = -BOX_D*0.3 + Math.random() * BOX_D * 0.3;
    }
    const clamped1 = clampInBox(x, z); x = clamped1.x; z = clamped1.z;
    const y = 0.28 + Math.random() * 0.05;
    const rx = (Math.random()-0.5) * 2.0;
    const ry = Math.random() * Math.PI * 2;
    const rz = (Math.random()-0.5) * 1.0;
    const p = createPlushie(type, new THREE.Vector3(x, y, z), {x:rx, y:ry, z:rz});
    plushies.push(p);
    plushieGroup.add(p);
  }

  // Layer 2: stacked on top, slightly fewer
  const layer2Count = 10;
  for(let i = 0; i < layer2Count; i++){
    const typeIdx = (i+3) % PLUSHIE_TYPES.length;
    const type = PLUSHIE_TYPES[typeIdx];
    let x = playCX + (Math.random()-0.5) * playSpanX * 0.7;
    let z = (Math.random()-0.5) * BOX_D * 0.55;
    if(isInHole(x, z)){
      x = playMinX + Math.random() * playSpanX * 0.5;
      z = -BOX_D*0.3 + Math.random() * BOX_D * 0.3;
    }
    const clamped2 = clampInBox(x, z); x = clamped2.x; z = clamped2.z;
    const y = 0.55 + Math.random() * 0.15;
    const rx = (Math.random()-0.5) * 2.2;
    const ry = Math.random() * Math.PI * 2;
    const rz = (Math.random()-0.5) * 1.2;
    const p = createPlushie(type, new THREE.Vector3(x, y, z), {x:rx, y:ry, z:rz});
    plushies.push(p);
    plushieGroup.add(p);
  }

  // 2 plushies near the prize hole walls (outside, touchable by claw swing)
  // Hole is flush to right wall, so place: 1=left side, 2=front side only
  const nearHolePositions = [
    { x: HOLE_POS.x - (holeW/2 + wallT + 0.35), z: HOLE_POS.z },                    // left of hole
    { x: HOLE_POS.x + (Math.random()-0.5)*0.3,   z: HOLE_POS.z - (holeD/2 + wallT + 0.35) }  // front of hole
  ];
  for(let i = 0; i < 2; i++){
    const type = PLUSHIE_TYPES[i];
    let x = nearHolePositions[i].x;
    let z = nearHolePositions[i].z;
    const clampedH = clampInBox(x, z); x = clampedH.x; z = clampedH.z;
    const y = 0.28;
    const rx = (Math.random()-0.5) * 1.5;
    const ry = Math.random() * Math.PI * 2;
    const rz = (Math.random()-0.5) * 0.6;
    const p = createPlushie(type, new THREE.Vector3(x, y, z), {x:rx, y:ry, z:rz});
    p.userData.nearHole = true;
    p.userData.vel = {x:0, z:0};
    plushies.push(p);
    plushieGroup.add(p);
  }
}
spawnPlushies();

// ─── Theme Selector ───
document.querySelectorAll('#charSelector button').forEach(btn=>{
  btn.addEventListener('click', ()=>{
    if(state !== State.IDLE) return; // 게임 중에는 변경 불가
    const theme = btn.dataset.theme;
    if(theme === currentTheme) return;
    currentTheme = theme;
    PLUSHIE_TYPES = PLUSHIE_THEMES[currentTheme];
    document.querySelectorAll('#charSelector button').forEach(b=>b.classList.remove('active'));
    btn.classList.add('active');
    spawnPlushies();
  });
});

// ─── Collection (localStorage) ───
const COLL_KEY = 'clawMachineCollection_v2';
let collection = JSON.parse(localStorage.getItem(COLL_KEY) || '[]');

const CHAR_ICONS = {
  shin:'🖍️', kazama:'📘', nene:'🎀', masao:'🌿', bo:'🤧', shiro:'🐶', maenggu:'🤧',
  cinna:'☁️', mocha:'🧁', cappuccino:'☕', espresso:'🫘', milk:'🥛', chiffon:'🍰',
  trex:'🦖', trice:'🦕', brachi:'🦒', stego:'🦎', ptera:'🦅', baby:'🥚',
  honey:'🍯', polar:'🐻‍❄️', brown:'🐻', panda:'🐼', strawberry:'🍓', sky:'☁️',
  hangyodon:'🐟', sayuri:'🐙', kingyo:'🐠', otamaro:'🦎', keroppi:'🐸', tuxedosam:'🐧',
  doraemon:'🐱', dorami:'🎀', nobita:'👓', shizuka:'👧', suneo:'🎤', gian:'🥊'
};
const THEME_NAMES = {shinchan:'짱구', cinnamoroll:'시나모롤', dino:'공룡', maenggu:'맹구', hangyodon:'한교동', doraemon:'도라에몽'};

function saveCollection(){
  localStorage.setItem(COLL_KEY, JSON.stringify(collection));
  renderCollection();
}
function addToCollection(name, char, theme){
  const existing = collection.find(c => c.char === char && c.theme === theme);
  if(existing){ existing.count++; }
  else { collection.push({name, char, theme, count:1}); }
  saveCollection();
}
function removeFromCollection(idx){
  const item = collection[idx];
  if(!item) return;
  if(item.count > 1){ item.count--; }
  else { collection.splice(idx, 1); }
  saveCollection();
}
// ─── Collection Thumbnail System (최적화: WebGL 컨텍스트 고갈 방지) ───
const thumbnailCache = {};
let thumbnailRenderer = null;

function getPlushieThumbnail(char, themeId) {
  const key = `${themeId}_${char}`;
  if (thumbnailCache[key]) return thumbnailCache[key];

  // 공유 렌더러가 없으면 생성
  if (!thumbnailRenderer) {
    thumbnailRenderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
    thumbnailRenderer.setSize(120, 120);
  }

  const theme = PLUSHIE_THEMES[themeId];
  if(!theme) return '';
  const t = theme.find(p => p.char === char);
  if(!t) return '';

  const scene = new THREE.Scene();
  const camera = new THREE.PerspectiveCamera(45, 1, 0.1, 100);
  camera.position.set(0, 0.7, 2.8);
  camera.lookAt(0, 0.6, 0);

  scene.add(new THREE.AmbientLight(0xffffff, 1.2));
  const dl = new THREE.DirectionalLight(0xffffff, 0.8);
  dl.position.set(1, 2, 3);
  scene.add(dl);

  const plushie = createPlushie(t, new THREE.Vector3(0, 0, 0), new THREE.Euler(0, 0.3, 0)); // 약간 사선으로 렌더링
  plushie.scale.set(1.2, 1.2, 1.2);
  scene.add(plushie);

  thumbnailRenderer.render(scene, camera);
  const dataUrl = thumbnailRenderer.domElement.toDataURL();
  
  // 메모리 정리
  plushie.traverse(obj => {
    if(obj.geometry) obj.geometry.dispose();
    if(obj.material) {
      if(Array.isArray(obj.material)) obj.material.forEach(m => m.dispose());
      else obj.material.dispose();
    }
  });

  thumbnailCache[key] = dataUrl;
  return dataUrl;
}

function renderCollection(){
  const list = document.getElementById('collectionList');
  const badge = document.getElementById('collBadge');
  const total = collection.reduce((s,c) => s + c.count, 0);
  badge.textContent = total;
  badge.style.display = total > 0 ? 'inline' : 'none';
  
  if(collection.length === 0){
    list.innerHTML = '<div id="collectionEmpty">아직 뽑은 인형이 없어요!<br>🎮 게임을 시작해보세요</div>';
    return;
  }

  list.innerHTML = collection.map((c, i) => {
    const imgUrl = getPlushieThumbnail(c.char, c.theme);
    return `
      <div class="coll-item">
        <button class="coll-del" data-idx="${i}">&times;</button>
        <div class="plushie-3d-canvas">
          <img src="${imgUrl}" style="width:100%; height:100%; object-fit:contain;" />
        </div>
        <span class="coll-name">${c.name}</span>
        <span class="coll-theme">${THEME_NAMES[c.theme] || ''}</span>
        <span class="coll-count">x${c.count}</span>
      </div>
    `;
  }).join('');

  list.querySelectorAll('.coll-del').forEach(btn => {
    btn.addEventListener('click', e => {
      e.stopPropagation();
      removeFromCollection(parseInt(btn.dataset.idx));
    });
  });
}
function toggleCollection(open){
  const panel = document.getElementById('collectionPanel');
  const backdrop = document.getElementById('collectionBackdrop');
  const isOpen = open !== undefined ? open : !panel.classList.contains('open');
  panel.classList.toggle('open', isOpen);
  backdrop.classList.toggle('open', isOpen);
}
document.getElementById('collectionToggle').addEventListener('click', () => toggleCollection());
document.getElementById('collectionClose').addEventListener('click', () => toggleCollection(false));
document.getElementById('collectionBackdrop').addEventListener('click', () => toggleCollection(false));
renderCollection();

// ─── UI Updates ───
const creditTxt = document.getElementById('creditTxt');
const timeTxt = document.getElementById('timeTxt');
const scoreTxt = document.getElementById('scoreTxt');
const msgOverlay = document.getElementById('msgOverlay');
const joyStick = document.getElementById('joyStick');

function updateUI(){
  creditTxt.textContent = 'CREDIT: ' + credits;
  timeTxt.textContent = 'TIME: ' + (timer > 0 ? timer : '--');
  scoreTxt.textContent = 'SCORE: ' + score;
}

function showMessage(text, duration=2000){
  msgOverlay.textContent = text;
  msgOverlay.style.opacity = '1';
  setTimeout(()=>{ msgOverlay.style.opacity = '0'; }, duration);
}

// ─── Nickname ───
const NICK_KEY = 'clawMachineNickname';
let playerName = localStorage.getItem(NICK_KEY) || '';
const nickOverlay = document.getElementById('nicknameOverlay');
const nickInput = document.getElementById('nicknameInput');
const nickEditBtn = document.getElementById('nicknameEdit');

function nn(){ return playerName ? playerName+'아' : '친구야'; }
function updateNickUI(){
  nickEditBtn.textContent = playerName ? '👤 '+playerName : '👤 닉네임 설정';
}

function saveNickname(){
  const val = nickInput.value.trim();
  if(val){
    playerName = val;
    localStorage.setItem(NICK_KEY, playerName);
    nickOverlay.classList.remove('open');
    updateNickUI();
    showMessage('🌟 '+nn()+' 반가워! START 눌러봐!', 3000);
  }
}

// Show modal if no nickname
if(!playerName){
  nickOverlay.classList.add('open');
  setTimeout(()=>nickInput.focus(), 300);
} else {
  updateNickUI();
}

nickInput.addEventListener('keydown', e=>{ if(e.key==='Enter') saveNickname(); });
document.getElementById('nicknameSave').addEventListener('click', saveNickname);
nickEditBtn.addEventListener('click', ()=>{
  nickInput.value = playerName;
  nickOverlay.classList.add('open');
  setTimeout(()=>nickInput.focus(), 100);
});

// ─── Game Logic ───
function insertCoin(){
  if(state !== State.IDLE) return;
  credits += 3;
  updateUI();
  showMessage('💰 '+nn()+'~ 코인 충전했어!', 1500);
}

function startGame(){
  if(state !== State.IDLE) return;
  if(credits <= 0){
    showMessage('💰 코인을 넣어줘!', 1500);
    return;
  }
  credits--;
  state = State.MOVING;
  timer = 30;
  craneX = DROP_ZONE.x; craneZ = DROP_ZONE.z;
  craneTargetX = DROP_ZONE.x; craneTargetZ = DROP_ZONE.z;
  wireY = 0; clawOpen = 1;
  pendulumAngleX = 0; pendulumAngleZ = 0;
  pendulumVelX = 0; pendulumVelZ = 0;
  grabbedPlushie = null;
  updateUI();
  showMessage('🎮 '+nn()+' 화이팅!', 1500);

  if(timerInterval) clearInterval(timerInterval);
  timerInterval = setInterval(()=>{
    if(state === State.MOVING){
      timer--;
      updateUI();
      if(timer <= 0){
        dropClaw();
      }
    }
  }, 1000);
}

function dropClaw(){
  if(state !== State.MOVING) return;
  if(timerInterval){ clearInterval(timerInterval); timerInterval = null; }
  state = State.DROPPING;
}

// ─── Input ───
const keys = {};
window.addEventListener('keydown', e=>{
  keys[e.key.toLowerCase()] = true;
  if(e.key === ' ' || e.key === 'Space'){
    e.preventDefault();
    if(state === State.IDLE) startGame();
    else if(state === State.MOVING) dropClaw();
  }
  if(e.key.toLowerCase() === 'c' && state === State.IDLE) insertCoin();
});
window.addEventListener('keyup', e=>{ keys[e.key.toLowerCase()] = false; });

// Panel joystick click/touch
const panelJoy = document.getElementById('panelJoystick');
let panelJoyActive = false, panelJoyId = null;

function handlePanelJoy(cx, cy){
  const rect = panelJoy.getBoundingClientRect();
  const dx = (cx - rect.left - rect.width/2) / (rect.width/2);
  const dy = (cy - rect.top - rect.height/2) / (rect.height/2);
  const len = Math.sqrt(dx*dx+dy*dy);
  if(len > 0.2){
    inputDir.x = Math.max(-1, Math.min(1, dx / Math.max(len, 0.5)));
    inputDir.z = Math.max(-1, Math.min(1, dy / Math.max(len, 0.5)));
  } else {
    inputDir.x = 0; inputDir.z = 0;
  }
  // Animate joystick tilt
  joyStick.style.transform = `rotateX(${-inputDir.z*20}deg) rotateZ(${-inputDir.x*15}deg)`;
}

panelJoy.addEventListener('pointerdown', e=>{
  panelJoyActive = true;
  panelJoyId = e.pointerId;
  panelJoy.setPointerCapture(e.pointerId);
  handlePanelJoy(e.clientX, e.clientY);
});
panelJoy.addEventListener('pointermove', e=>{
  if(panelJoyActive && e.pointerId === panelJoyId) handlePanelJoy(e.clientX, e.clientY);
});
panelJoy.addEventListener('pointerup', e=>{
  if(e.pointerId === panelJoyId){
    panelJoyActive = false;
    inputDir.x = 0; inputDir.z = 0;
    joyStick.style.transform = '';
  }
});

// Start button
document.getElementById('coinBtn').addEventListener('click', ()=>{
  insertCoin();
});
document.getElementById('startBtn').addEventListener('click', ()=>{
  if(state === State.IDLE) startGame();
  else if(state === State.MOVING) dropClaw();
});

// Mobile touch joystick
const touchJoyArea = document.getElementById('touchJoystickArea');
const touchKnob = document.getElementById('touchKnob');
let touchJoyActive = false, touchJoyId = null;

function handleTouchJoy(cx, cy){
  const rect = touchJoyArea.getBoundingClientRect();
  const dx = cx - rect.left - rect.width/2;
  const dy = cy - rect.top - rect.height/2;
  const maxR = rect.width/2 - 24;
  const dist = Math.sqrt(dx*dx+dy*dy);
  const clampDist = Math.min(dist, maxR);
  const angle = Math.atan2(dy, dx);
  const nx = Math.cos(angle)*clampDist;
  const ny = Math.sin(angle)*clampDist;
  touchKnob.style.transform = `translate(calc(-50% + ${nx}px), calc(-50% + ${ny}px))`;

  if(dist > 15){
    inputDir.x = nx / maxR;
    inputDir.z = ny / maxR;
  } else {
    inputDir.x = 0; inputDir.z = 0;
  }
  // Also tilt panel joystick
  joyStick.style.transform = `rotateX(${-inputDir.z*20}deg) rotateZ(${-inputDir.x*15}deg)`;
}

touchJoyArea.addEventListener('touchstart', e=>{
  e.preventDefault();
  touchJoyActive = true;
  touchJoyId = e.changedTouches[0].identifier;
  const t = e.changedTouches[0];
  handleTouchJoy(t.clientX, t.clientY);
}, {passive:false});
touchJoyArea.addEventListener('touchmove', e=>{
  e.preventDefault();
  for(const t of e.changedTouches){
    if(t.identifier === touchJoyId) handleTouchJoy(t.clientX, t.clientY);
  }
}, {passive:false});
touchJoyArea.addEventListener('touchend', e=>{
  for(const t of e.changedTouches){
    if(t.identifier === touchJoyId){
      touchJoyActive = false;
      inputDir.x = 0; inputDir.z = 0;
      touchKnob.style.transform = 'translate(-50%,-50%)';
      joyStick.style.transform = '';
    }
  }
});

// Mobile grab button
document.getElementById('touchGrabBtn').addEventListener('click', ()=>{
  if(state === State.IDLE) startGame();
  else if(state === State.MOVING) dropClaw();
});

// ─── Animation Loop ───
const clock = new THREE.Clock();

function animate(){
  requestAnimationFrame(animate);
  const dt = Math.min(clock.getDelta(), 0.05);

  // Keyboard input
  if(!panelJoyActive && !touchJoyActive){
    let kx = 0, kz = 0;
    if(keys['a'] || keys['arrowleft']) kx = -1;
    if(keys['d'] || keys['arrowright']) kx = 1;
    if(keys['w'] || keys['arrowup']) kz = -1;
    if(keys['s'] || keys['arrowdown']) kz = 1;
    inputDir.x = kx;
    inputDir.z = kz;
    joyStick.style.transform = (kx||kz) ? `rotateX(${-kz*20}deg) rotateZ(${-kx*15}deg)` : '';
    // Auto-start on joystick input
    if((kx||kz) && state === State.IDLE) startGame();
  }

  // Auto-start on touch/panel joystick input
  if((inputDir.x || inputDir.z) && state === State.IDLE && (panelJoyActive || touchJoyActive)) startGame();

  if(state === State.MOVING){
    const prevX = craneX, prevZ = craneZ;
    craneX += inputDir.x * CRANE_SPEED;
    craneZ += inputDir.z * CRANE_SPEED;
    craneX = Math.max(-MOVE_BOUNDS, Math.min(MOVE_BOUNDS, craneX));
    craneZ = Math.max(-MOVE_BOUNDS, Math.min(MOVE_BOUNDS, craneZ));

    // Pendulum physics from crane acceleration
    const accX = craneX - prevX;
    const accZ = craneZ - prevZ;
    pendulumVelX -= accX * PENDULUM_IMPULSE / (dt||0.016);
    pendulumVelZ -= accZ * PENDULUM_IMPULSE / (dt||0.016);

    // Circular joystick detection — adds rotational swing to pendulum
    const inputLen = Math.sqrt(inputDir.x*inputDir.x + inputDir.z*inputDir.z);
    if(inputLen > 0.3){
      const curAngle = Math.atan2(inputDir.z, inputDir.x);
      let angleDiff = curAngle - prevInputAngle;
      // Normalize to -PI..PI
      if(angleDiff > Math.PI) angleDiff -= Math.PI*2;
      if(angleDiff < -Math.PI) angleDiff += Math.PI*2;
      inputAngularVel = inputAngularVel * 0.7 + angleDiff * 0.3;
      prevInputAngle = curAngle;
      // Apply circular swing force — perpendicular to current pendulum angle
      const swingForce = inputAngularVel * 0.15 * inputLen;
      pendulumVelX += Math.cos(curAngle + Math.PI/2) * swingForce;
      pendulumVelZ += Math.sin(curAngle + Math.PI/2) * swingForce;
    } else {
      inputAngularVel *= 0.9;
    }
  }

  // Pendulum always updates
  pendulumVelX -= pendulumAngleX * PENDULUM_GRAVITY;
  pendulumVelZ -= pendulumAngleZ * PENDULUM_GRAVITY;
  pendulumVelX *= PENDULUM_DAMPING;
  pendulumVelZ *= PENDULUM_DAMPING;
  pendulumAngleX += pendulumVelX;
  pendulumAngleZ += pendulumVelZ;
  pendulumAngleX = Math.max(-0.5, Math.min(0.5, pendulumAngleX));
  pendulumAngleZ = Math.max(-0.5, Math.min(0.5, pendulumAngleZ));

  // Drop / Grab / Rise sequence
  if(state === State.DROPPING){
    wireY += DROP_SPEED;

    // Check if claw reached a plushie while descending
    const clawTipY = craneGroup.position.y - (1.2 + wireY * (BOX_H - 1.8)) - 0.6;
    const craneWorldX = PLAY_OFFSET + craneX * (PLAY_W/2 * 0.85);
    const craneWorldZ = craneZ * (BOX_D/2 * 0.85);
    let hitPlushie = null;

    for(const p of plushies){
      if(p.userData.grabbed || p.userData.falling) continue;
      const dx = p.position.x - craneWorldX;
      const dz = p.position.z - craneWorldZ;
      const distXZ = Math.sqrt(dx*dx+dz*dz);
      // Check horizontal proximity AND vertical — claw tip near plushie top
      if(distXZ < p.userData.radius * 1.5 && clawTipY < p.position.y + 0.4){
        hitPlushie = p;
        break;
      }
    }

    if(hitPlushie || wireY >= 1){
      // Stop descending — clamp
      if(wireY > 1) wireY = 1;
      state = State.GRABBING;
      clawOpen = 0;

      // Determine grab success
      if(hitPlushie && Math.random() < GRAB_PROBABILITY){
        grabbedPlushie = hitPlushie;
        grabbedPlushie.userData.grabbed = true;
      }
      setTimeout(()=>{ state = State.RISING; }, 600);
    }
  }

  if(state === State.RISING){
    wireY -= RISE_SPEED;
    // Random chance to drop plushie while rising
    if(grabbedPlushie && Math.random() < 0.004){
      grabbedPlushie.userData.grabbed = false;
      grabbedPlushie.userData.falling = true;
      grabbedPlushie.userData.slipped = true;
      grabbedPlushie.userData.fallVelY = -0.005;
      grabbedPlushie.userData.fallVelX = (Math.random()-0.5)*0.02;
      grabbedPlushie.userData.fallVelZ = (Math.random()-0.5)*0.02;
      grabbedPlushie.userData.fallSpin = (Math.random()-0.5)*0.08;
      grabbedPlushie = null;
      showMessage('😢 '+nn()+' 미끄러졌어!', 1500);
    }
    if(wireY <= 0){
      wireY = 0;
      state = State.RETURNING;
    }
  }

  if(state === State.RETURNING){
    // Random chance to drop plushie while moving to exit
    if(grabbedPlushie && Math.random() < 0.003){
      grabbedPlushie.userData.grabbed = false;
      grabbedPlushie.userData.falling = true;
      grabbedPlushie.userData.slipped = true;
      grabbedPlushie.userData.fallVelY = -0.005;
      grabbedPlushie.userData.fallVelX = (Math.random()-0.5)*0.03;
      grabbedPlushie.userData.fallVelZ = (Math.random()-0.5)*0.03;
      grabbedPlushie.userData.fallSpin = (Math.random()-0.5)*0.1;
      grabbedPlushie = null;
      showMessage('😢 '+nn()+' 아깝다!', 1500);
    }
    // Move crane to drop zone
    const dx = DROP_ZONE.x - craneX;
    const dz = DROP_ZONE.z - craneZ;
    const dist = Math.sqrt(dx*dx+dz*dz);
    if(dist > 0.02){
      craneX += (dx/dist) * CRANE_SPEED * 1.5;
      craneZ += (dz/dist) * CRANE_SPEED * 1.5;
    } else {
      clawOpen = 1;
      if(grabbedPlushie){
        const fallingPlushie = grabbedPlushie;
        fallingPlushie.userData.falling = true;
        fallingPlushie.userData.fallVelY = 0;
        fallingPlushie.userData.fallVelX = (Math.random()-0.5)*0.02;
        fallingPlushie.userData.fallVelZ = (Math.random()-0.5)*0.02;
        fallingPlushie.userData.fallSpin = (Math.random()-0.5)*0.1;
        grabbedPlushie = null;
        showMessage('🎉 '+nn()+' 축하해! 뽑았다!', 2000);
      } else {
        showMessage('😢 '+nn()+' 다시 해보자!', 1200);
      }
      // 딜레이 없이 바로 다음 게임 가능
      state = State.IDLE;
      craneX = DROP_ZONE.x; craneZ = DROP_ZONE.z;
      if(plushies.length < 12) spawnPlushies();
    }
  }

  // Update claw open/close
  const targetOpen = (state === State.GRABBING || state === State.RISING || state === State.RETURNING) ? 0 : 1;
  clawOpen += (targetOpen - clawOpen) * 0.1;

  // ─── Update 3D objects ───
  // Crane position
  const worldX = PLAY_OFFSET + craneX * (PLAY_W/2 * 0.85);
  const worldZ = craneZ * (BOX_D/2 * 0.85);
  craneGroup.position.x = worldX;
  craneGroup.position.z = worldZ;

  // Z-rail follows crane X
  zRail.position.x = worldX;

  // Floor indicator follows crane XZ
  indicatorGroup.position.x = worldX;
  indicatorGroup.position.z = worldZ;
  // Pulse animation during MOVING state
  if(state === State.MOVING){
    const pulse = 0.4 + Math.sin(performance.now()*0.006)*0.15;
    ringMat.opacity = pulse;
    crossMat.opacity = pulse * 0.8;
    dotMat.opacity = pulse + 0.2;
    indicatorGroup.visible = true;
  } else if(state === State.DROPPING || state === State.GRABBING){
    ringMat.opacity = 0.6;
    crossMat.opacity = 0.5;
    dotMat.opacity = 0.7;
    indicatorGroup.visible = true;
  } else {
    indicatorGroup.visible = false;
  }

  // Wire length — longer base so claw hangs lower during MOVING
  const wireLen = 1.2 + wireY * (BOX_H - 1.8);
  wire.scale.y = wireLen;
  wire.position.y = -wireLen/2 - 0.1;

  // Claw pivot position and pendulum
  clawPivot.position.y = -wireLen - 0.15;
  clawPivot.rotation.x = pendulumAngleZ;
  clawPivot.rotation.z = -pendulumAngleX;

  // Finger spread — prongs splay outward when open
  fingers.forEach((f, i)=>{
    const a = f.userData.baseAngle;
    const spread = clawOpen * 0.4;
    // Move outward from center
    f.position.x = Math.cos(a) * (0.1 + spread);
    f.position.z = Math.sin(a) * (0.1 + spread);
    // Tilt outward (open) or inward (closed)
    f.rotation.x = -Math.sin(a) * clawOpen * 0.45;
    f.rotation.z = Math.cos(a) * clawOpen * 0.45;
  });

  // Grabbed plushie follows claw
  if(grabbedPlushie){
    const clawWorldPos = new THREE.Vector3();
    clawPivot.getWorldPosition(clawWorldPos);
    grabbedPlushie.position.x += (clawWorldPos.x - grabbedPlushie.position.x) * 0.15;
    grabbedPlushie.position.y += (clawWorldPos.y - 0.3 - grabbedPlushie.position.y) * 0.15;
    grabbedPlushie.position.z += (clawWorldPos.z - grabbedPlushie.position.z) * 0.15;
  }

  // ─── Claw push physics for hole-adjacent plushies ───
  const clawWorldPos3 = new THREE.Vector3();
  clawPivot.getWorldPosition(clawWorldPos3);
  const clawSwingSpeed = Math.sqrt(pendulumVelX*pendulumVelX + pendulumVelZ*pendulumVelZ);

  for(let i = plushies.length - 1; i >= 0; i--){
    const p = plushies[i];
    if(!p.userData.nearHole) continue;
    if(p.userData.grabbed) continue;

    // Check if swinging claw is near this plushie
    const dxC = p.position.x - clawWorldPos3.x;
    const dzC = p.position.z - clawWorldPos3.z;
    const distC = Math.sqrt(dxC*dxC + dzC*dzC);

    if(distC < 0.6 && clawSwingSpeed > 0.02 && state === State.MOVING && timer < 27){
      // Push plushie away from claw swing direction (requires deliberate swinging)
      const pushForce = clawSwingSpeed * 0.5;
      if(!p.userData.vel) p.userData.vel = {x:0, z:0};
      p.userData.vel.x += (dxC/distC) * pushForce;
      p.userData.vel.z += (dzC/distC) * pushForce;
    }

    // Update plushie velocity
    if(p.userData.vel){
      p.userData.vel.x *= 0.92;
      p.userData.vel.z *= 0.92;
      p.position.x += p.userData.vel.x * 0.02;
      p.position.z += p.userData.vel.z * 0.02;

      // Clamp to box walls — prevent escaping the machine
      const bndX = BOX_W/2 - 0.2;
      const bndZ = BOX_D/2 - 0.2;
      if(p.position.x > bndX){ p.position.x = bndX; p.userData.vel.x *= -0.3; }
      if(p.position.x < -bndX){ p.position.x = -bndX; p.userData.vel.x *= -0.3; }
      if(p.position.z > bndZ){ p.position.z = bndZ; p.userData.vel.z *= -0.3; }
      if(p.position.z < -bndZ){ p.position.z = -bndZ; p.userData.vel.z *= -0.3; }

      // ─── Hole wall collision ───
      const relX = p.position.x - HOLE_POS.x;
      const relZ = p.position.z - HOLE_POS.z;
      const halfW = holeW/2 + wallT;
      const halfD = holeD/2 + wallT;
      const innerHalfW = holeW/2;
      const innerHalfD = holeD/2;
      const plR = 0.2; // plushie collision radius

      // Check if plushie is trying to enter the hole box from outside
      const insideWallX = Math.abs(relX) < halfW + plR;
      const insideWallZ = Math.abs(relZ) < halfD + plR;
      const insideInnerX = Math.abs(relX) < innerHalfW;
      const insideInnerZ = Math.abs(relZ) < innerHalfD;

      if(insideWallX && insideWallZ && !(insideInnerX && insideInnerZ)){
        // Plushie is hitting a wall — push it out
        if(Math.abs(relX) > innerHalfW && insideWallZ){
          const sign = relX > 0 ? 1 : -1;
          p.position.x = HOLE_POS.x + sign * (halfW + plR);
          p.userData.vel.x *= -0.3; // bounce
        }
        if(Math.abs(relZ) > innerHalfD && insideWallX){
          const sign = relZ > 0 ? 1 : -1;
          p.position.z = HOLE_POS.z + sign * (halfD + plR);
          p.userData.vel.z *= -0.3; // bounce
        }
      }

      // Check if plushie is inside the hole (past the walls, over the opening)
      // Only count as a score during active gameplay (MOVING state)
      if(insideInnerX && insideInnerZ && !p.userData.falling){
        if(state === State.MOVING || state === State.DROPPING || state === State.GRABBING || state === State.RISING || state === State.RETURNING){
          p.userData.falling = true;
          p.userData.fallVelY = -0.005;
          p.userData.fallVelX = p.userData.vel.x * 0.3;
          p.userData.fallVelZ = p.userData.vel.z * 0.3;
          p.userData.fallSpin = (Math.random()-0.5)*0.08;
          p.userData.nearHole = false;
          showMessage('🎉 '+nn()+' 대박! 흔들어서 뽑았어!', 2000);
        } else {
          // IDLE state — push plushie back out of hole, don't count
          const pushX = relX === 0 ? 0.5 : (relX > 0 ? 1 : -1);
          const pushZ = relZ === 0 ? 0.5 : (relZ > 0 ? 1 : -1);
          p.position.x = HOLE_POS.x + pushX * (halfW + plR + 0.1);
          p.position.z = HOLE_POS.z + pushZ * (halfD + plR + 0.1);
          p.userData.vel.x = pushX * 0.02;
          p.userData.vel.z = pushZ * 0.02;
        }
      }
      // Gravity toward hole when very close — only during active game
      else if(state !== State.IDLE && insideWallX && insideWallZ && insideInnerX){
        p.userData.vel.z -= relZ * 0.01;
      } else if(state !== State.IDLE && insideWallX && insideWallZ && insideInnerZ){
        p.userData.vel.x -= relX * 0.01;
      }
    }
  }

  // ─── Falling plushie physics (gravity + tumble) ───
  for(let i = plushies.length - 1; i >= 0; i--){
    const p = plushies[i];
    if(!p.userData.falling) continue;

    // Gravity
    p.userData.fallVelY -= 0.003; // gravity acceleration
    p.position.y += p.userData.fallVelY;
    p.position.x += p.userData.fallVelX;
    p.position.z += p.userData.fallVelZ;

    // Clamp falling plushie inside box walls
    const fbX = BOX_W/2 - 0.15;
    const fbZ = BOX_D/2 - 0.15;
    if(p.position.x > fbX){ p.position.x = fbX; p.userData.fallVelX *= -0.3; }
    if(p.position.x < -fbX){ p.position.x = -fbX; p.userData.fallVelX *= -0.3; }
    if(p.position.z > fbZ){ p.position.z = fbZ; p.userData.fallVelZ *= -0.3; }
    if(p.position.z < -fbZ){ p.position.z = -fbZ; p.userData.fallVelZ *= -0.3; }

    // Tumble rotation
    p.rotation.x += p.userData.fallSpin;
    p.rotation.z += p.userData.fallSpin * 0.7;

    // 미끄러진 인형은 바닥 또는 다른 인형 위에 착지
    if(p.userData.slipped){
      let landY = 0.28;
      for(const other of plushies){
        if(other === p || other.userData.falling) continue;
        const ldx = other.position.x - p.position.x;
        const ldz = other.position.z - p.position.z;
        if(Math.sqrt(ldx*ldx + ldz*ldz) < 0.5){
          landY = Math.max(landY, other.position.y + 0.3);
        }
      }
      if(p.position.y <= landY){
        p.position.y = landY;
        p.userData.falling = false;
        p.userData.slipped = false;
        p.userData.fallVelY = 0;
        p.userData.fallVelX = 0;
        p.userData.fallVelZ = 0;
        p.userData.fallSpin = 0;
        p.scale.set(1,1,1);
        continue;
      }
    }

    // Shrink as it falls into the hole (not slipped)
    if(p.position.y < 0.1 && !p.userData.slipped){
      p.scale.multiplyScalar(0.96);
    }

    // Remove when well below floor — score + collection
    if(p.position.y < -1.0){
      score++;
      updateUI();
      addToCollection(p.userData.type, p.userData.char, p.userData.theme);
      plushieGroup.remove(p);
      plushies.splice(i, 1);
    }
  }

  // LED animation on top light
  const t = performance.now() * 0.001;
  topLight.intensity = 1.3 + Math.sin(t*3)*0.2;

  renderer.render(scene, camera);
}

updateUI();
if(playerName) showMessage('🌟 '+nn()+' 반가워! START 눌러봐!', 3000);
animate();