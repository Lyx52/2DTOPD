import processing.core.*; 
import processing.data.*;
import java.util.ArrayList;

public class main extends PApplet {
  public static void main(String[] args) {
    PApplet.main("main", args);
  }

  private int playerSize = 0, //Player colbox size
              tileSize = 0, //Invdividual tile size
              maxZombies = 10, //Max zombies spawned at one time
              lastBiteFrameCount = 0, //Last frame count since zombie has bitten
              mapHeight = 256, mapWidth = 256, //Map width & height
              gameHours = 0, gameMinutes = 0; //Game time

  private player p; //Player object
  private PImage survivor, survivorFeet, //Survivor images
                 icon_knife, icon_handgun, icon_rifle, icon_ammo, icon_ammo_pack, //Icons
                 spritesheet_boom; //Spritesheets

  private String gameState = "game"; //game;paused;menu;settings
  private XML gameWeapons;
  private tile[][] grid = new tile[mapWidth][mapHeight];

//Object Lists 
private ArrayList<GameObject> object = new ArrayList<>();
private ArrayList<enemy> ai = new ArrayList<>();

private float lastTime = 0,
              deltaTime = 0,
              mapOffsetX = 1,
              mapOffsetY = 1;
public void settings() {
  int resOpt = 0; //0 = SVGA; 1 = XGA; 2 = FHD         
  switch (resOpt) {
    case 0 : {
      size(800, 600, JAVA2D);
      playerSize = 50;
      break;
    }
    case 1 : {
      size(1024, 768, JAVA2D);
      playerSize = 65;
      break;
    }
    case 2 : {
      size(1920, 1080, JAVA2D);
      playerSize = 80;
      break;
    }
    default : {
      size(800,600, JAVA2D);
      break;
    }
  }
  tileSize = PApplet.parseInt(((width * height) / (4*(resOpt + 1))) * 0.0003f);
}
public void setup() {
  gameWeapons = loadXML("src/data/xml/game_weapons.xml");
  XML gameItems = loadXML("src/data/xml/game_items.xml");
  surface.setTitle("Game Alpha Version");
  frameRate(60); 
  rectMode(CENTER);
  imageMode(CENTER);
  p = new player(new PVector(400,300));
  icon_knife = loadImage("src/data/sprites/icons/knife_icon.png", "png");
  icon_handgun = loadImage("src/data/sprites/icons/handgun_icon.png", "png");
  icon_rifle = loadImage("src/data/sprites/icons/rifle_icon.png", "png");
  icon_ammo = loadImage("src/data/sprites/icons/ammo_icon.png", "png");
  icon_ammo_pack = loadImage("src/data/sprites/icons/icon_ammo_pack-min.png", "png");
  spritesheet_boom = loadImage("src/data/sprites/spritesheets/boom-min.png", "png");
  generateGrid();
  loadGrid();
}

public void draw() {
  if (p.health <= 0) {
    p = new player(new PVector(400,300));  
  } //Respawn player
  if (focused && !gameState.equals("paused")) {
    background(220);
    getDeltaTime();
    p.update();
    drawUI();
    translate(mapOffsetX, mapOffsetY);
    pushMatrix();
      if (grid.length > 0) {
        for (int row = 0; row < mapHeight; row++) {
          for (int col = 0; col < mapWidth; col++) {
            if (dist(grid[col][row].pos.x * tileSize + (tileSize >> 1), grid[col][row].pos.y * tileSize + (tileSize >> 1), p.pos.x, p.pos.y) <= 485) {
              grid[col][row].display();
            }
          }
        }  
      }
      spawnZombie();
      if (ai.size() > 0) {
        for (int i = 0; i < ai.size(); i++) {
          if (dist(ai.get(i).pos.x, ai.get(i).pos.y, p.pos.x, p.pos.y) <= 500) {
            ai.get(i).update();
          }
          if (dist(ai.get(i).pos.x, ai.get(i).pos.y, p.pos.x, p.pos.y) <= PApplet.parseInt(playerSize)) {
            if (frameCount - lastBiteFrameCount >= 15 && p.health > 0) {
            
              p.health -= ceil(random(2,7));
              p.hit = true;
              if (p.health < 0) {
                p.health = 0;  
              }
              lastBiteFrameCount = frameCount;
            } 
          }
          if (ai.get(i).health <= 0) {
            ai.remove(i);
          }
        }
      }
      if (object.size() > 0) {
        //println(obj.size());
        for (int i = 0; i < object.size(); i++) {
          GameObject tmp = object.get(i);
          if (tmp.remove) {
            object.remove(i);
            if (tmp.nObjType == 1) {
              p.magCounter++;
            }
          }
          tmp.update();  
        }
      }
    popMatrix();
    displayFPS();
  }
}

  class GameObject {
  PVector pos, startPos;
  int nAmmoPackCollisionSize = 20, nBulletCollisionSize = 5,
      nObjType = 0;
      
  float angf;
  
  final float accf = 0.4f;
  
  boolean knife = false, remove = false;
  
  GameObject(PVector p, float a, boolean k, int ot) {
    pos = p;
    startPos = p;
    angf = a;
    knife = k;
    nObjType = ot;
  }
  void update() {
    switch(nObjType) {
      case 0 : {     
        display(nObjType, nBulletCollisionSize);
        pos.x += angf == PI ? -(accf * deltaTime) : angf == 0 ? accf * deltaTime : 0;
        pos.y += angf == -HALF_PI ? -(accf * deltaTime) : angf == HALF_PI ? accf * deltaTime : 0;
        remove = (inBounds(pos.x, pos.y) || (knife && (dist(pos.x, pos.y, p.pos.x, p.pos.y) >= (playerSize * 0.75f)))) || ((pos.x > tileSize) && (pos.y > tileSize) && grid[floor(pos.x / tileSize)][floor(pos.y / tileSize)].wall);
        for (int i = 0; i < ai.size(); i++) {
          if (dist(pos.x , pos.y , ai.get(i).pos.x , ai.get(i).pos.y) <= PApplet.parseInt(playerSize /1.4f)) {
            p.dealDamage(ai.get(i));
            remove = true;
          }
        }
        break;
      }
      case 1 : {
        if (dist(pos.x, pos.y, p.pos.x, p.pos.y) <= 500) {
          display(nObjType, nAmmoPackCollisionSize);
          remove = getEllipseCollision(p.pos, pos, (nAmmoPackCollisionSize / 2) + (playerSize / 2)) && p.magCounter <= p.maxPlayerMags;
        }
        break;
      }
      default :
    }
  }
  private boolean inBounds(float xpos, float ypos) {
      if (xpos < mapOffsetX || xpos+mapOffsetX > width) {
        return true;
      }
      return ypos < mapOffsetY || ypos + mapOffsetY > height;
    }
  void display(int COLLISION_TYPE, int COLLISION_SIZE) {
    switch(COLLISION_TYPE) {
      case 0 : {
        pushMatrix();
          int nTempx = 0, nTempy = 0;
          if (angf == PI) {
            nTempx -= p.spriteSize/2; 
            nTempy -= p.spriteSize/4;
          } else if (angf == 0) {
            nTempx += p.spriteSize/2;
            nTempy += p.spriteSize/4;  
          } else if (angf == -HALF_PI) {
            nTempx += p.spriteSize/4;
            nTempy -= p.spriteSize/2;           
          } else if (angf == HALF_PI) {
            nTempx -= p.spriteSize/4;
            nTempy += p.spriteSize/2; 
          }
          
          translate(nTempx + pos.x, nTempy + pos.y);
          
          rotate(angf);
          if (!knife) {
            circle(0, 0, COLLISION_SIZE);
          }
        popMatrix();
        break;
      }
      case 1 : {
        tint(255, 190);
        image(icon_ammo_pack,pos.x ,pos.y,nAmmoPackCollisionSize*1.5f,nAmmoPackCollisionSize);
        tint(255, 255);
        break;
      }
      default :
    }
  }
}
  class enemy {
  PVector pos, //Enemy position
          waypoint, //Next enemy waypoint
          tmp;
  int dir = 0, prevDist = 0, health = 100, lastFrameCount = 0;
  float vx = 0, vy = 0, ang = 0, tang = 0;
  final float acc_f = p.acc_walk_f*0.8f, 
              drag_f = 0.9f, 
              maxVel = p.maxVel*0.9f,
              PLAYER_SIGHT_RANGE = 200;
  
  ray r;
  boolean left = true, right = true , up = true, down = true, 
          allowLeft = true, allowRight = true, allowUp = true, allowDown = true,
          canSeePlayer = false, atWaypoint = true;
  
  enemy(PVector p) {
    pos = p;
    lastFrameCount = frameCount;
    r = new ray(PApplet.parseInt(pos.x), PApplet.parseInt(pos.y), tang , this);
  }
  void update() {
    display();
    tang = atan2((p.pos.y-pos.y), (p.pos.x-pos.x));
    float temp = random(1);   
    checkNearbyPlayer();
    //If player has damaged the enemy it becomes agressive
    canSeePlayer = health < 100 && dist(p.pos.x, p.pos.y, pos.x, pos.y) <= 500 || canSeePlayer;
    
    if (canSeePlayer) {
      if (dist(p.pos.x, p.pos.y, pos.x, pos.y) >= 200 && health == 100) {
        canSeePlayer = false; 
        genWaypoint();
      }
      ang = tang;
      moveTo(p.pos);
    } else if (frameCount % 5 == 0) {
      if (temp >= 0.76f) {
        changeRotation();
      } else if (temp <= 0.75f) {
        if (atWaypoint) {
          genWaypoint();
        }
      }
    } else if (waypoint != null) {
      moveTo(waypoint); 
      if (frameCount % 10 == 0) {
        if (tmp != null) {
          if (dist(pos.x, pos.y, tmp.x, tmp.y) <= 10 && !canSeePlayer) {
            genWaypoint();  
          }
        }
        tmp = new PVector(pos.x, pos.y);    
      }
      atWaypoint = dist(pos.x, pos.y, waypoint.x, waypoint.y) <= 5 && waypoint != null || waypoint.x > tileSize && waypoint.y > tileSize && grid[floor(waypoint.x / tileSize)][floor(waypoint.y / tileSize)].wall;
    }
    
    if (pos.x + vx > tileSize * 2 && pos.y + vy > tileSize * 2) {
      vx = vx < 0 && left && grid[floor((pos.x + vx - (playerSize >> 1)) / tileSize)][floor((pos.y + vy) / tileSize)].wall ? 0 : vx;
      vx = vx > 0 && right && grid[floor((pos.x + vx + (playerSize >> 1)) / tileSize)][floor((pos.y + vy) / tileSize)].wall ? 0 : vx;
      
      vy = vy < 0 && up && grid[floor((pos.x + vx) / tileSize)][floor((pos.y + vy - (playerSize >> 1)) / tileSize)].wall ? 0 : vy;
      vy = vy > 0 && down && grid[floor((pos.x + vx) / tileSize)][floor((pos.y + vy + (playerSize >> 1)) / tileSize)].wall ? 0 : vy;
    }
    
    pos.x += vx;
    pos.y += vy;
    
    vx *= drag_f;
    vy *= drag_f;
  }
  void genWaypoint() {
    int moveDist = 50, 
        tempx = dir == 0 ? -moveDist : dir == 1 ? moveDist : 0, 
        tempy = dir == 2 ? -moveDist : dir == 3 ? moveDist : 0;
    waypoint = new PVector(PApplet.parseInt(random(pos.x, pos.x+tempx)),PApplet.parseInt(random(pos.y, pos.y+tempy)));
  }
  
  
  void checkNearbyPlayer() {
    while (dist(r.pos.x, r.pos.y, pos.x, pos.y) <= PLAYER_SIGHT_RANGE*(PApplet.parseInt(p.playerGunState == 2) + 1)) {
      r.update();
      if (r.remove) { break; }  
    } 
    r = new ray(PApplet.parseInt(pos.x), PApplet.parseInt(pos.y), tang, this);
   
  }
  
  
  void changeRotation() {
    switch(dir) {
      case 0 : {
        //DOWN
        dir = 2;
        ang = -HALF_PI;
        break;
      }
      case 2 : {
        //RIGHT
        dir = 1;
        ang = 0;
        break;
      }
      case 1 : {
        //UP
        dir = 3;
        ang = HALF_PI;
        break;
      }
      case 3 : {
        //LEFT
        dir = 0;
        ang = PI;
        break;
      }
    }
  }
  
  void moveTo(PVector p) {
    vx += pos.x < p.x && vx < maxVel ? acc_f*deltaTime*PApplet.parseInt(right)*PApplet.parseInt(allowRight) : pos.x > p.x && allowLeft && vx > -maxVel ? -(acc_f*deltaTime*PApplet.parseInt(left)*PApplet.parseInt(allowLeft)) : 0;
    vy += pos.y < p.y && vy < maxVel ? acc_f*deltaTime*PApplet.parseInt(down)*PApplet.parseInt(allowDown) : pos.y > p.y && allowLeft && vy > -maxVel ? -(acc_f*deltaTime*PApplet.parseInt(up)*PApplet.parseInt(allowUp)) : 0;
  }
  void display() {
    pushMatrix();
    
      translate(pos.x,pos.y);
      
      if (health != 100) {
        fill(255,0,0);
        rect(0,35,50,10);  
      }
      
      rectMode(CORNER);
        fill(0,255,0);
        rect(-25,30, health >> 1,10);
        
      rectMode(CENTER);
        rotate(ang);
        
        fill(255);
        circle(0 ,0 , playerSize);
        
        fill(255,0,0);
        circle(20, 0, playerSize >> 3);
        
    popMatrix();
    fill(255);
  }
}
  class player {
    PVector pos;
  PGraphics ch;

  ray r;

  String[] handgunLoc = new String[4],
           rifleLoc = new String[4],
           knifeLoc = new String[4];

  float velx_f, vely_f, lastAng, spriteSize = 0;

  final float acc_walk_f = 0.015f, acc_run_f = 0.025f, drag_f = 0.9f, maxVel = 4;
  int lastSprite = 0, lastShotSprite = 0, lastReloadSprite = 0,
      ammoCounter = 0,magCounter = 0, lastBoomSprite = 0,
      maxPlayerMags = 5, lastSpriteFrames = 0,

      playerState = 0, //0 - Idle 1; - Moving
      playerGun = 0, //0 - handgun; 1 - rifle; 2 - knife
      playerGunState = 0, //0 - Nothing; 2 - shooting; 3 - reloading
      //Player stats
      health = 100, stamina = 100, staminaUpgrade = 1, agilityUpgrade = 1,

      //HANDGUN
      handgunAmmo = 0, handgunMags = 0, handgunDamage = 0, handgunAmmoPerMag = 0, lastFrameHandgun = 0,

      //RIFLE
      rifleAmmo = 0, rifleMags = 0, rifleDamage = 0, rifleAmmoPerMag = 0, lastFrameRifle = 0,

      //KNIFE
      knifeDamage = 30, lastFrameKnife = 0;
  boolean up = false, down = false,
          left = false, right = false,
          running = false, enableAuto = false,
          enableBoomSS = false, regenStamina = false,
          hit = false, shoot = false;

  player(PVector p) {
    pos = new PVector(p.x, p.y);
    spriteSize = playerSize * 1.4f;
    animator();
    ch = createGraphics(PApplet.parseInt(spriteSize),PApplet.parseInt(spriteSize));

    //Handgun
    handgunAmmoPerMag = gameWeapons.getChild("handgun").getInt("ammopermag");
    handgunMags = (gameWeapons.getChild("handgun").getInt("defaultammo") - handgunAmmoPerMag) / handgunAmmoPerMag;
    handgunAmmo = handgunAmmoPerMag;
    handgunDamage = gameWeapons.getChild("handgun").getInt("damage");

    //Rifle
    rifleAmmoPerMag = gameWeapons.getChild("rifle").getInt("ammopermag");
    rifleMags = (gameWeapons.getChild("rifle").getInt("defaultammo") - rifleAmmoPerMag) / rifleAmmoPerMag;
    rifleAmmo = rifleAmmoPerMag;
    rifleDamage = gameWeapons.getChild("rifle").getInt("damage");

    for (int i = 0; i < 4; i++) {
      String temp = "";
      switch (i) {
        case 0 : temp = "idleLoc";
        case 1 : temp = "moveLoc";
        case 2 : temp = "shootLoc";
        case 3 : temp = "reloadLoc";
      }

      knifeLoc[i] = !temp.equals("reloadLoc") ? gameWeapons.getChild("mele").getString(temp) : null;
      rifleLoc[i] = gameWeapons.getChild("rifle").getString(temp);
      handgunLoc[i] = gameWeapons.getChild("handgun").getString(temp);
    }

    ammoCounter = handgunAmmo;
    magCounter = handgunMags;
  }
  void update() {
    mapOffsetX = (width >> 1) - pos.x;
    mapOffsetY = (height >> 1) - pos.y;

    playerState = velx_f != 0 || vely_f != 0 ? 1 : 0;

    if (enableAuto && playerGun == 1) {
      shoot();
    }
    display();
    if (running && stamina > 0 && !regenStamina) {
      velx_f += velx_f <= maxVel && velx_f >= -maxVel ?(PApplet.parseInt(right) - PApplet.parseInt(left)) * acc_run_f * deltaTime : 0;
      vely_f += vely_f <= maxVel && vely_f >= -maxVel ?(PApplet.parseInt(down) - PApplet.parseInt(up)) * acc_run_f * deltaTime : 0;
      stamina --;
    } else {
      velx_f += velx_f <= maxVel && velx_f >= -maxVel ?(PApplet.parseInt(right) - PApplet.parseInt(left)) * acc_walk_f * deltaTime : 0;
      vely_f += vely_f <= maxVel && vely_f >= -maxVel ?(PApplet.parseInt(down) - PApplet.parseInt(up)) * acc_walk_f * deltaTime : 0;
      if (stamina != 100) {
        running = false;
        stamina ++;
        regenStamina = true;
      } else regenStamina = false;
    }

    velx_f = pos.x+velx_f > playerSize >> 1 && pos.x+velx_f < mapWidth * tileSize ? velx_f : 0;
    vely_f = pos.y+vely_f > playerSize >> 1 && pos.y+vely_f < mapHeight * tileSize ? vely_f : 0;

    if (pos.x + velx_f > tileSize * 2 && pos.y + vely_f > tileSize * 2) {
      velx_f = velx_f < 0 && left && grid[floor((pos.x + velx_f - (playerSize >> 1)) / tileSize)][floor((pos.y + vely_f) / tileSize)].wall ? 0 : velx_f;
      velx_f = velx_f > 0 && right && grid[floor((pos.x + velx_f + (playerSize >> 1)) / tileSize)][floor((pos.y + vely_f) / tileSize)].wall ? 0 : velx_f;

      vely_f = vely_f < 0 && up && grid[floor((pos.x + velx_f) / tileSize)][floor((pos.y + vely_f - (playerSize >> 1)) / tileSize)].wall ? 0 : vely_f;
      vely_f = vely_f > 0 && down && grid[floor((pos.x + velx_f) / tileSize)][floor((pos.y + vely_f + (playerSize >> 1)) / tileSize)].wall ? 0 : vely_f;

    }

    pos.x += velx_f;
    pos.y += vely_f;

    velx_f *= velx_f == 0 || velx_f > -0.05f && velx_f < 0 || velx_f < 0.05f && velx_f > 0 ? 0 : drag_f;
    vely_f *= vely_f == 0 || vely_f > -0.05f && vely_f < 0 || vely_f < 0.05f && vely_f > 0 ? 0 : drag_f;
  } //Generally update player
  void dealDamage(enemy e) {
    float temp = random(1);
    if (temp <= 0.8f) {
      if (playerGun == 0) {
        e.health -= handgunDamage;
      } else if (playerGun == 1) {
        e.health -= rifleDamage;
      } else if (playerGun == 2) {
        e.health -= knifeDamage;
      }
    } else if (temp >= 0.81f) {
      //Crit Damage
      if (playerGun == 0) {
        e.health -= handgunDamage*1.5f;
      } else if (playerGun == 1) {
        e.health -= rifleDamage*1.5f;
      } else if (playerGun == 2) {
        e.health -= knifeDamage*1.5f;
      }
    }
  } //Deal damage to hit enemy
  void reload() {
    if (lastReloadSprite == 0) {
      if (playerGun == 0 && magCounter > 0 && ammoCounter != handgunAmmoPerMag) {
        playerGunState = 3;
        magCounter--;
        ammoCounter = handgunAmmoPerMag;
      } else if (playerGun == 1 && magCounter > 0 && ammoCounter != rifleAmmoPerMag) {
        playerGunState = 3;
        magCounter--;
        ammoCounter = rifleAmmoPerMag;
      }
    }
  } //Reload current weapon
  float updateAng() {
    if (left) {
      lastAng = PI;
    } else if (right) {
      lastAng = 0;
    } else if (up) {
      lastAng = -HALF_PI;
    } else if (down) {
      lastAng = HALF_PI;
    }
    return lastAng;
  } //Update player angle

  void shoot() {
    if (playerGun != 2) {
      if (playerGun == 0 && frameCount - lastFrameHandgun >= 20) {
        if (ammoCounter > 0 && playerGunState != 3) {
          ammoCounter--;
          object.add(new GameObject(new PVector(pos.x,pos.y),updateAng(), false, 0));
          playerGunState = 2;
          enableBoomSS = true;
          lastFrameHandgun = frameCount;
        } else if (ammoCounter <= 0) {
          playerGunState = 0;
          enableBoomSS = false;
          reload();
        }
      } else if (playerGun == 1 && frameCount - lastFrameRifle >= 10) {
        if (ammoCounter > 0 && playerGunState != 3) {
          ammoCounter--;
          object.add(new GameObject(new PVector(pos.x,pos.y),updateAng(), false , 0));
          playerGunState = 2;
          enableBoomSS = true;
          lastFrameRifle = frameCount;
        } else if (ammoCounter <= 0) {
          playerGunState = 0;
          enableBoomSS = false;
          reload();
        }
      }
    } else {
      if (frameCount - lastFrameRifle >= 25) {
        playerGunState = 2;
        object.add(new GameObject(new PVector(pos.x,pos.y),updateAng(), true , 0));
      }
    }
  }

  //Switch player weapons
  void switchWeapon() {
    lastShotSprite = 0;
    lastReloadSprite = 0;
    if (playerGun == 0) {
      playerGun = 1;
      handgunAmmo = ammoCounter;
      handgunMags = magCounter;
      ammoCounter = rifleAmmo;
      magCounter = rifleMags;
    } else if (playerGun == 1) {
      playerGun = 2;
      rifleAmmo = ammoCounter;
      rifleMags = magCounter;
    } else if (playerGun == 2) {
      playerGun = 0;
      ammoCounter = handgunAmmo;
      magCounter = handgunMags;
    }
  }

  void display() {
    switch(playerState) {
      case 0 : {
        if (frameCount - lastSpriteFrames >= 6) {
          animator();
        }
        break;
      }
      case 1 : {
        if (running && frameCount - lastSpriteFrames >= 1 || !running && frameCount - lastSpriteFrames >= 2 ) {
          animator();
        }
        break;
      }
    }
    pushMatrix();
    translate(width >> 1, height >> 1);
    if (enableBoomSS) {
      if (lastAng == PI) {
        image(spriteSheetAnimator(),-p.spriteSize/2, -p.spriteSize/4, 25, 25);
      } else if (lastAng == 0) {
        image(spriteSheetAnimator(),p.spriteSize/2, p.spriteSize/4, 25, 25);
      } else if (lastAng == -HALF_PI) {
        image(spriteSheetAnimator(),p.spriteSize/4, -p.spriteSize/2, 25, 25);
      } else if (lastAng == HALF_PI) {
        image(spriteSheetAnimator(),-p.spriteSize/4, p.spriteSize - (p.spriteSize / 3), 25, 25);
      }
    }
    if (playerGun != 2) {
      fill(255,0,0);
      textSize(13);
      text(magCounter+" : "+ammoCounter,10, 59);
      fill(255);
    }
    if (stamina != 100) {
      fill(255);
      rect(0,30,50,10);
    }
    rectMode(CORNER);
    fill(0,0,255);
    rect(0-25,25,stamina >> 1,8);
    rectMode(CENTER);
    fill(0);

    if (health != 100) {
      fill(255,0,0);
      rect(0,38,50,8);
    }
    rectMode(CORNER);
    fill(0,255,0);
    rect(0-25,34, health >> 1,8);
    rectMode(CENTER);
    fill(0);

    if (playerGun != 2) {
      image(icon_ammo,-20,53, playerSize / 2.5f, playerSize / 2.5f);
    }

    rotate(updateAng());
    if (hit) {
      tint(255,0,0);
      hit = false;
    } else tint(255);
    image(survivorFeet, 0, 3, spriteSize / 1.5f, spriteSize / 1.5f);
    if (playerGun != 2) {
      image(survivor, 0, 0,spriteSize,spriteSize);
    }
    if (playerGun == 2 && playerGunState != 2) {
      image(survivor, 0, 0,spriteSize,spriteSize);
    } else if (playerGunState == 2 && playerGun == 2) {
      image(survivor, 10, 7,spriteSize* 1.27f,spriteSize * 1.27f);
    }
    popMatrix();
  }

  PImage spriteSheetAnimator() {
    PImage temp = null;
    int tempw = PApplet.parseInt(spritesheet_boom.width/6),
        temph = PApplet.parseInt(spritesheet_boom.height >> 2);

    if (enableBoomSS && lastBoomSprite != 23) {
      temp = spritesheet_boom.get(tempw*floor(lastBoomSprite >> 2), temph*floor(lastBoomSprite/6), tempw, temph);
      if (playerGun != 1) {
        lastBoomSprite += 2;
      } else lastBoomSprite += 3;
      if (lastBoomSprite >= 23) {
        enableBoomSS = false;
        lastBoomSprite = 0;
      }
    }
    return temp;
  } //Animate spritesheet
  void animator() {

    //Walking/Running
    if (lastSprite >= 19) {
      lastSprite = 0;
    } else if (playerState == 1) {

      //Feet
      survivorFeet = !running ? loadImage("src/data/sprites/feet/walk/survivor-walk_"+lastSprite+"-min.png", "png") : loadImage("src/data/sprites/feet/run/survivor-run_"+lastSprite+"-min.png", "png");

      //HANDGUN WALKING
      if (playerGunState == 0) {
        switch (playerGun) {
          case 0 : {
            survivor = loadImage("src/data/sprites/handgun/move/survivor-move_handgun_"+lastSprite+"-min.png", "png");
            break;
          }
          case 1 : {
            survivor = loadImage("src/data/sprites/rifle/move/survivor-move_rifle_"+lastSprite+".png", "png");
            break;
          }
          case 2 : {
            survivor = loadImage("src/data/sprites/knife/move/survivor-move_knife_"+lastSprite+"-min.png", "png");
            break;
          }
        }
      }
    } else if (playerState == 0) {

      //Feet
      survivorFeet = loadImage("src/data/sprites/feet/idle/survivor-idle_0.png", "png");

      //HANDGUN IDLE
      if (playerGunState == 0) {
        switch(playerGun) {
          case 0 : {
            survivor = loadImage("src/data/sprites/handgun/idle/survivor-idle_handgun_"+lastSprite+"-min.png", "png");
            break;
          }
          case 1 : {
            survivor = loadImage("src/data/sprites/rifle/idle/survivor-idle_rifle_"+lastSprite+"-min.png", "png");
            break;
          }
          case 2 : {
            survivor = loadImage("src/data/sprites/knife/idle/survivor-idle_knife_"+lastSprite+".png", "png");
            break;
          }
        }
      }
    }
    lastSprite++;

    //WALKING OR IDLE
    switch(playerGun) {
      case 0 : {
        switch(playerGunState) {
          case 2 : {
            if (lastShotSprite == 3) {
              lastShotSprite = 0;
              playerGunState = 0;
            } else {
              survivor = loadImage("src/data/sprites/handgun/shoot/survivor-shoot_handgun_"+lastShotSprite+".png", "png");
              lastShotSprite++;
            }
            break;
          }
          case 3 : {
            if (lastReloadSprite == 14) {
              playerGunState = 0;
              lastReloadSprite = 0;
            } else {
              survivor = loadImage("src/data/sprites/handgun/reload/survivor-reload_handgun_"+lastReloadSprite*PApplet.parseInt(playerGunState == 3)+"-min.png", "png");
              lastReloadSprite++;
            }
            break;
          }
        }
        break;
      }
      case 1 : {
        switch(playerGunState) {
          case 2 : {
            if (lastShotSprite == 3) {
              lastShotSprite = 0;
              playerGunState = 0;
            } else {
              survivor = loadImage("src/data/sprites/rifle/shoot/survivor-shoot_rifle_"+lastShotSprite+".png", "png");
              lastShotSprite++;
            }
            break;
          }
          case 3 : {
            if (lastReloadSprite == 19) {
              playerGunState = 0;
              lastReloadSprite = 0;
            } else {
              survivor = loadImage("src/data/sprites/rifle/reload/survivor-reload_rifle_"+lastReloadSprite*PApplet.parseInt(playerGunState == 3)+".png", "png");
              lastReloadSprite++;
            }
            break;
          }
        }
        break;
      }
      case 2 : {
        if (playerGunState == 2) {
          if (lastShotSprite == 14) {
            lastShotSprite = 0;
            playerGunState = 0;
          } else {
            survivor = loadImage("src/data/sprites/knife/attack/survivor-meleeattack_knife_"+lastShotSprite+"-min.png", "png");
            lastShotSprite++;
          }
        }
        break;
      }
    }
  } //Animate images
}

  //Classes
  public class tile {
    //Tile position on map
    PVector pos;
    //Is tile a wall?
    boolean wall = false;
    tile (PVector p) {
      pos = p;
    }
    private void display() {
      if (wall) {
        fill(255);
      } else noFill();
      pushMatrix();
        stroke(0);
        translate(pos.x * tileSize + (tileSize >> 1), pos.y * tileSize + (tileSize >> 1));
        rect(0,0, tileSize, tileSize);
      popMatrix();
    }
  } //Tile object
  class ray {
    PVector pos; //Ray position
    enemy e; //Enemy that shot ray
    float acc = 5, //Ray acceleration
          ang; //Ray angle
    boolean remove = false; //Flag to remove ray
    ray(int ENEMY_X_POSITION, int ENEMY_Y_POSITION, float ENEMY_ANGLE, enemy THIS_ENEMY) {
      pos = new PVector (ENEMY_X_POSITION, ENEMY_Y_POSITION);
      ang = ENEMY_ANGLE;
      e = THIS_ENEMY;
    }
    void update() {
      pos.x += (cos(ang) * acc);
      pos.y += (sin(ang) * acc);
      e.canSeePlayer = dist(pos.x, pos.y, p.pos.x, p.pos.y) <= playerSize >> 1 || (!(pos.x > tileSize) || !(pos.y > tileSize) || !(pos.x > tileSize) || !(pos.y > tileSize) || !grid[floor(pos.x / tileSize)][floor(pos.y / tileSize)].wall) && e.canSeePlayer;
      remove = pos.x > tileSize && pos.y > tileSize && pos.x > tileSize && pos.y > tileSize && grid[floor(pos.x / tileSize)][floor(pos.y / tileSize)].wall || dist(pos.x, pos.y, p.pos.x, p.pos.y) <= playerSize >> 1;
    }
  } //Enemy rays

  //Functions
  private PVector getRandomVector() {
    /* wall 0 = left, 1 = up, 2 = right, 3 = down */
    int x, y;
    switch (parseInt(random(4))) {
      case 0 : {
        x = parseInt(random(-playerSize,0));
        y = parseInt(random(0,height));
        break;
      }
      case 1 : {
        x = parseInt(random(0,width));
        y = parseInt(random(-playerSize,0));
        break;
      }
      case 2 : {
        x = parseInt(random(width,width+ playerSize));
        y = parseInt(random(0,height));
        break;
      }
      case 3 : {
        x = parseInt(random(0,width));
        y = parseInt(random(height,height+ playerSize));
        break;
      }
      default:
        throw new IllegalStateException("Unexpected value: " + parseInt(random(4)));
    }
    return new PVector(x,y);
  } //Get random position for spawned enemy
  private boolean getEllipseCollision(PVector POSITION1, PVector POSITION2, int MINIMAL_DISTANCE) {
    return dist(POSITION1.x, POSITION1.y, POSITION2.x, POSITION2.y) <= MINIMAL_DISTANCE;
  } //Check if 2 ellipses collide with distance
  private void spawnZombie() {
    if (frameCount % 30 == 0 && ai.size() <= maxZombies) ai.add(new enemy(getRandomVector()));
  } //Spawn new zombie
  private void drawUI() {
    noFill();
    if (p.playerGun == 0) {
      stroke(255,0,0);
    } else stroke(0);
    rect(width- playerSize,height/3, playerSize -10, playerSize -10);
    image(icon_handgun,width- playerSize,height/3, playerSize -13, playerSize -13);

    if (p.playerGun == 1) {
      stroke(255,0,0);
    } else stroke(0);
    rect(width- playerSize, (height / 3) + 100, playerSize -10, playerSize -10);
    image(icon_rifle,width- playerSize, (height / 3) + 100, playerSize -13, playerSize -13);

    if (p.playerGun == 2) {
      stroke(255,0,0);
    } else stroke(0);
    rect(width- playerSize, (height / 3) + 200, playerSize -10, playerSize -10);
    image(icon_knife,width- playerSize, (height / 3) + 200, playerSize -13, playerSize -13);
    stroke(0);
  } //Draw game ui
  private void generateGrid() {
    for (int row = 0; row < mapHeight; row++) {
      for (int col = 0; col < mapWidth; col++) {
        grid[col][row] = new tile(new PVector(col,row));
      }
    }
  } //Generate grid
  private void loadGrid() {
    byte[] mapArray = loadBytes("src/data/mapc/maps/map.dat");
    if (grid.length > 0) {
      int t = 0;
      for (int row = 0; row < mapHeight; row++) {
        for (int col = 0; col < mapWidth; col++) {
          grid[col][row].wall = PApplet.parseBoolean(mapArray[t]);
          t++;
        }
      }
    }
  } //Load map
  private void getDeltaTime() {
    float currentTime = millis();
    if (lastTime == 0) {
      lastTime = currentTime;
      deltaTime = 0;
    } else {
      deltaTime = currentTime - lastTime;
      lastTime = currentTime;
    }
  } //Get current delta time
  private void displayFPS() {
    fill(0);
    text("FPS: "+ parseInt(frameRate),15 - mapOffsetX, height-15 - mapOffsetY);
  } //Display fps counter
  private boolean updateTime() {
    gameMinutes = (frameCount % 3 == 0) ? gameMinutes + 1 : gameMinutes >= 60 ? 0 : gameMinutes;
    gameHours = gameMinutes >= 60 ? gameHours + 1 : gameHours >= 24 ? 0 : gameHours;
    if (gameHours >= 6 && gameHours <= 18) {
      return true;
    } else return true;
  } //Update time

  public void keyPressed() {
    switch(keyCode) {
      case 32 : {
        p.shoot = true;
        break;
      }
      case 85 : {
        //Add ammopack
        object.add(new GameObject(new PVector(100,100) ,0, false,1));
        break;
      }
      case 80 : {
        gameState = "paused";
        break;
      }
      case 65 : {
        p.left = true;
        break;
      }
      case 68 : {
        p.right = true;
        break;
      }
      case 87 : {
        p.up = true;
        break;
      }
      case 83 : {
        p.down = true;
      }
      case 9 : {
        p.switchWeapon();
        break;
      }
      default :
    }
  }
  public void keyReleased() {
    switch(keyCode) {
      case 32 : {
        p.shoot = false;
        break;
      }
      case 65 : {
        p.left = false;
        break;
      }
      case 68 : {
        p.right = false;
        break;
      }
      case 87 : {
        p.up = false;
        break;
      }
      case 83 : {
        p.down = false;
      }
      default :
    }
  }
}
