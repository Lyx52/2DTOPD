int tileSize = 32,
    rows = 256, cols = 256,
    btnCount = 1;
byte[] map = new byte[cols * rows];
boolean wallMode = false;

String btn1 = "Wall";

ArrayList<button> btn = new ArrayList<button>();
float mapOffsetX = 0, mapOffsetY = 0;
tile[][] grid = new tile[cols][rows];
void setup() {
  textSize(20);
  size(800,800,P2D);
  rectMode(CENTER);
  genGrid();
  for (int i = 0; i < btnCount; i++) {
    if (i == 0) {
      btn.add(new button(new PVector(50,750),80,60,btn1));
    } 
  }
}

void draw() {
  background(220); 
  if (wallMode && mousePressed) {
    println(wallMode);
    tile temp = grid[floor((pmouseX - mapOffsetX)/tileSize)][floor((pmouseY - mapOffsetY)/tileSize)];
    temp.wall = mouseButton == LEFT? true : false;
  }
  for (int i = 0; i < btnCount; i++) {
    if (btn.get(i) != null) {
      btn.get(i).update();
    }
  }
  if (grid.length > 0) {
    for (int row = 0; row < rows; row++) {
      for (int col = 0; col < cols; col++) {
        grid[col][row].display();
      }
    }  
  }
}

void keyPressed() {
  checkKey(keyCode, true);
}
void keyReleased() {
  checkKey(keyCode, false);  
}
void mousePressed() {
  for (int i = 0; i < btn.size(); i++) {
    btn.get(i).btnpress(true, mouseX, mouseY);  
  }
}
void loadMap() {
  map = loadBytes("maps/map.dat");
  if (grid.length > 0) {
    int t = 0;
    for (int row = 0; row < rows; row++) {
      for (int col = 0; col < cols; col++) {
        grid[col][row].wall = boolean(map[t]);
        t++;
      }
    }  
  }  
}
void saveMap() {
  if (grid.length > 0) {
    int t = 0;
    for (int row = 0; row < rows; row++) {
      for (int col = 0; col < cols; col++) {
        if ( grid[col][row].wall) {
          map[t] = 1;
        } else map[t] = 0;
        t++;
      }
    }  
  }
  saveBytes("maps/map.dat", map);
}

void checkKey(int kc, boolean b) {
  switch(kc) {
    case 68 : {
      mapOffsetX -= (50*int(b));
      break; 
    }
    case 65 : {
      mapOffsetX += (50*int(b));
      break;
    }
    case 87 : {
      mapOffsetY += (50*int(b));
      break;
    }
    case 83 : {
      mapOffsetY -= (50*int(b));
      break;
    }
    case 77 : {
      saveMap();
      break;
    }
    case 78 : {
      loadMap(); {
        
      }
    }
    default:;
  }  
}
void genGrid() {
  for (int row = 0; row < rows; row++) {
    for (int col = 0; col < cols; col++) {
      grid[col][row] = new tile(new PVector(col,row));  
    }
  }
}
