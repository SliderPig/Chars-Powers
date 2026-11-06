# The pet ping's texture: plain white, the 3D arrow gets all its colour from its corners (mobcontrol/client/PetHud).
# python tools/make_pet_art.py
from PIL import Image
Image.new('RGBA', (4, 4), (255, 255, 255, 255)).save('src/main/resources/assets/charpowers/textures/gui/pet_ping.png')
