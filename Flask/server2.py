import os
from flask import Flask, render_template, request
import tensorflow as tf
from keras.models import load_model
import numpy as np
from PIL import Image, ImageChops
import cv2


model = tf.keras.models.load_model('model_p')

app = Flask(__name__)

app.config["UPLOADS"] = "./source"


@app.route('/')
def home():
    return "Home page"

def div(img):
    h = img.shape[0]
    w = img.shape[1]
    w_cutoff = w//2
    right1 = img[:, w_cutoff:]
    img = cv2.rotate(right1, cv2.ROTATE_90_CLOCKWISE)
    h = img.shape[0]
    w = img.shape[1]
    w_cutoff = w//2
    r1 = img[:, :w_cutoff]
    r1 = cv2.rotate(r1, cv2.ROTATE_90_COUNTERCLOCKWISE)
    return r1

@app.route('/predict', methods=['GET','POST'])
def predict_img():
    if request.method == 'POST':
        if request.files:
            for f in request.files:
                pf = request.files[f]
                img = Image.open(pf)
                img = img.convert("L")
                #img = ImageChops.invert(img)
                img1 = img.resize((28,28))
                img2 = np.array(img1)
                img2 = div(img2)
                img2 = img2.reshape(-1,14,14,1)
                img2 = img2/255.0
                pred = model.predict(img2)
                num = np.argmax(pred)
                confidence_value = str(pred.item(num))
                #act_img = request.files[f]
                #act_img.seek(0)
                #act_img.save(os.path.join(app.config["UPLOADS"], num,act_img.filename))
                res = str(num)+":"+confidence_value
            return res
    else:
        return "This is the source folder"

if __name__ == '__main__':
    app.run(host="0.0.0.0", port=5002, debug=True)
