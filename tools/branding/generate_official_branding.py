"""Deterministically package the official PNG, preserving its colored pixels.
Requires Pillow/numpy. Does not overwrite the source or use network/generative art.
"""
from pathlib import Path
from collections import deque
import hashlib,json,math
import numpy as np
from PIL import Image,ImageDraw,ImageFilter
ROOT=Path(__file__).resolve().parents[2]
RES=ROOT/'app/src/main/res'; OUT=RES/'drawable-nodpi';OUT.mkdir(exist_ok=True)
SOURCE=RES/'drawable/logo.png'
source=Image.open(SOURCE).convert('RGBA');bounds=source.getbbox();art=source.crop(bounds)
pixels=np.array(art);alpha=pixels[:,:,3]
arrow=(pixels[:,:,0].astype(int)>pixels[:,:,1].astype(int))&(alpha>0)
mask=(alpha>64)&~arrow;seen=np.zeros(mask.shape,bool);road_core=np.zeros(mask.shape,np.uint8)
for y,x in zip(*np.where(mask)):
 if seen[y,x]:continue
 todo=deque([(int(y),int(x))]);seen[y,x]=True;points=[]
 while todo:
  a,b=todo.popleft();points.append((a,b))
  for c,d in ((a-1,b),(a+1,b),(a,b-1),(a,b+1)):
   if 0<=c<mask.shape[0] and 0<=d<mask.shape[1] and mask[c,d] and not seen[c,d]:
    seen[c,d]=True;todo.append((c,d))
 # Three original isolated road markings; retain their exact outlines and colors.
 if 1000<len(points)<6000:
  for a,b in points:road_core[a,b]=255
road=np.array(Image.fromarray(road_core).filter(ImageFilter.MaxFilter(7)))>0
road&=(alpha>0)&~arrow
layers=[]
for selection in (~(road|arrow),road,arrow):
 layer=pixels.copy();layer[~selection]=0;layers.append(Image.fromarray(layer))
assert np.array_equal(sum(np.array(p).astype(np.uint16) for p in layers),pixels)
yy,xx=np.where(alpha>0)
radius=np.sqrt((xx-art.width/2)**2+(yy-art.height/2)**2).max()
def canvas(image,side,safe_radius):
 factor=safe_radius/radius;size=(round(art.width*factor),round(art.height*factor))
 result=Image.new('RGBA',(side,side))
 result.paste(image.resize(size,Image.Resampling.LANCZOS),((side-size[0])//2,(side-size[1])//2))
 return result
launcher=canvas(art,432,128) # 108dp canvas within the circular 66dp safe area.
launcher.save(OUT/'official_launcher_color.png')
mono=Image.new('RGBA',launcher.size,'white');mono.putalpha(launcher.getchannel('A'));mono.save(OUT/'official_launcher_mono.png')
# Identical registration for native 288dp canvas and the Compose image.
canvas(art,864,276).save(OUT/'official_startup_logo.png')
for name,layer in zip(('base','road','arrow'),layers):canvas(layer,864,276).save(OUT/f'official_startup_{name}.png')
for name,png in (('ic_launcher_foreground','official_launcher_color'),('ic_launcher_monochrome','official_launcher_mono'),('splash_icon','official_startup_logo')):
 (RES/f'drawable/{name}.xml').write_text(f'''<?xml version="1.0" encoding="utf-8"?>
<bitmap xmlns:android="http://schemas.android.com/apk/res/android"
    android:src="@drawable/{png}" android:gravity="fill" android:filter="true" />
''',encoding='utf-8')
for density,side in (('mdpi',48),('hdpi',72),('xhdpi',96),('xxhdpi',144),('xxxhdpi',192)):
 folder=RES/f'mipmap-{density}';folder.mkdir(exist_ok=True)
 for rounded in (False,True):
  full=side*4;background=Image.new('RGBA',(full,full),'#123D32');background.alpha_composite(canvas(art,full,full*.43))
  shape=Image.new('L',(full,full));draw=ImageDraw.Draw(shape)
  if rounded:draw.ellipse((0,0,full-1,full-1),fill=255)
  else:draw.rounded_rectangle((0,0,full-1,full-1),radius=full*.22,fill=255)
  background.putalpha(shape)
  background.resize((side,side),Image.Resampling.LANCZOS).save(folder/f'ic_launcher{"_round" if rounded else ""}.png')
delivery=ROOT/'delivery/official-branding';delivery.mkdir(parents=True,exist_ok=True)
sheet=Image.new('RGB',(960,580));draw=ImageDraw.Draw(sheet)
for row,wallpaper in enumerate(('#F5F1E9','#0E211B')):
 draw.rectangle((0,row*290,960,(row+1)*290),fill=wallpaper)
 for col,label in enumerate(('Circle','Squircle','Rounded square')):
  side=180;mask=Image.new('L',(side,side));md=ImageDraw.Draw(mask)
  if col==0:md.ellipse((0,0,179,179),fill=255)
  elif col==2:md.rounded_rectangle((0,0,179,179),radius=40,fill=255)
  else:
   points=[]
   for i in range(361):
    a=math.radians(i);c,s=math.cos(a),math.sin(a)
    points.append((90+89*math.copysign(abs(c)**.5,c),90+89*math.copysign(abs(s)**.5,s)))
   md.polygon(points,fill=255)
  icon=Image.new('RGBA',(side,side),'#123D32')
  icon.alpha_composite(launcher.resize((270,270),Image.Resampling.LANCZOS).crop((45,45,225,225)))
  icon.putalpha(mask);x=70+col*310;y=35+row*290;sheet.paste(icon,(x,y),icon)
  draw.text((x,y+194),label,fill='#123D32' if row==0 else '#F5F1E9')
sheet.save(delivery/'launcher-masks.png')
(delivery/'source.json').write_text(json.dumps({'source':str(SOURCE.relative_to(ROOT)),'sha256':hashlib.sha256(SOURCE.read_bytes()).hexdigest(),'original_size':source.size,'alpha_bounds':bounds,'source_modified':False,'layer_recomposition':'pixel-exact'},indent=2))
print('Packaged official logo: adaptive, monochrome, legacy, native and animation layers.')
