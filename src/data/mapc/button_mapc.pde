class button {
  PVector pos;
  
  boolean selected = false;
  
  int btnWidth = 0, btnHeight = 0;
  
  String title = "";
  button(PVector p, int bw, int bh, String str) {
    pos = p;
    btnWidth = bw;
    btnHeight = bh;
    title = str;
  }
  
  void update() {
    display();  
    wallMode = title == "Wall" && selected ? true : false;
  }
  void btnpress(boolean b, int x, int y) {
    if (b) {
      if (x >= pos.x - btnWidth/2 && x <= pos.x + btnWidth/2) {
        if (y >= pos.y - btnHeight/2 && y <= pos.y + btnHeight/2) {
          if (selected) {
            selected = false;  
          } else selected = b;
        }
      }
    }
  }
  void display() {
    
    if (selected) {
      stroke(255,0,0);
      strokeWeight(3);
    } else {
      stroke(0);
      strokeWeight(0);  
    }
    
    fill(255); 
    tint(200);
    rect(pos.x, pos.y, btnWidth, btnHeight); 
    
    fill(0);
    text(title,pos.x-17, pos.y+9);
    
    strokeWeight(1);
    stroke(0);
  }
}
