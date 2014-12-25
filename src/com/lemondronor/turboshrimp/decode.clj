(ns com.lemondronor.turboshrimp.decode
  (:import (com.twilight.h264.decoder AVFrame AVPacket H264Decoder MpegEncContext)
           com.twilight.h264.player.FrameUtils
           java.util.Arrays
           java.awt.image.BufferedImage))


(def INBUF_SIZE 65535)
(def inbuf-int (int-array (+ INBUF_SIZE MpegEncContext/FF_INPUT_BUFFER_PADDING_SIZE)))
(def ^AVPacket avpkt (AVPacket.))
(def ^H264Decoder codec (H264Decoder.))
(def ^MpegEncContext c (MpegEncContext/avcodec_alloc_context))
(def ^AVFrame picture (AVFrame/avcodec_alloc_frame))

(defn init-decoder []
  (do
    (.av_init_packet avpkt)
    (if (nil? codec) (println "Codec not found"))
    (if (not (= 0 (bit-and (.capabilities codec) H264Decoder/CODEC_CAP_TRUNCATED)))
      (println "need to configure CODEC_FLAG_TRUNCATED"))
    (if (< (.avcodec_open c codec) 0)
      (println "Could not open codec"))))


(defn to-ba-int [b]
  (doall (for [i (range 0 (count b))]
           (aset-int inbuf-int i (bit-and 0xFF (nth b i))))))

(defn convert! [got-picture]
  (let [len (.avcodec_decode_video2 c picture got-picture avpkt)]
    len))

(defn get-image-icon [^AVFrame picture buffer]
  (let [image  (BufferedImage.
                (.imageWidth picture) (.imageHeight picture) BufferedImage/TYPE_INT_RGB)]
    (do
      (.setRGB
       image 0 0 (.imageWidth picture) (.imageHeight picture) buffer 0 (.imageWidth picture))
      image)))

(defn convert-frame [b]
  (do
    (def got-picture (int-array [0]))
    (to-ba-int b)
    (set! (.size avpkt) (count b))
    (set! (.data_base avpkt) inbuf-int)
    (set! (.data_offset avpkt) 0)
    (if (> (convert! got-picture) 0)
      (if (first got-picture)
        (let [ picture (.displayPicture (.priv_data c))
              buffer-size (* (.imageHeight picture) (.imageWidth picture))
              buffer (int-array buffer-size)
              ]
          (do
            (FrameUtils/YUV2RGB picture buffer)
            (get-image-icon picture buffer)
            )
          )
        )
      (println "Could not decode frame"))))

(init-decoder)

