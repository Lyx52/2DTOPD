class tile {
  PVector pos;
  boolean wall = false;
  tile (PVector p) {
    pos = p;    
  }
  
  void display() {
    if (wall) {
      fill(255);  
    } else noFill();
    pushMatrix();
    translate(pos.x * tileSize + (tileSize / 2) + mapOffsetX, pos.y * tileSize + (tileSize / 2) + mapOffsetY);
    rect(0,0, tileSize, tileSize);
    popMatrix();
  }
}
