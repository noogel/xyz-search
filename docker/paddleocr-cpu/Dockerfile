FROM python:3.10

ENV PPOCR=v2.7.5
ENV DET=ch_PP-OCRv4_det_server_infer.tar
ENV CLS=ch_ppocr_mobile_v2.0_cls_slim_infer.tar
ENV REC=ch_PP-OCRv4_rec_server_infer.tar
ENV PROTOCOL_BUFFERS_PYTHON_IMPLEMENTATION=python

RUN git clone https://mirror.ghproxy.com/https://github.com/PaddlePaddle/PaddleOCR.git /PaddleOCR \
        && cd /PaddleOCR && git checkout tags/$PPOCR && mkdir -p /PaddleOCR/inference \
        && pip config set global.index-url https://pypi.tuna.tsinghua.edu.cn/simple \
        && pip install https://paddle-wheel.bj.bcebos.com/2.6.0/linux/linux-cpu-mkl-avx/paddlepaddle-2.6.0-cp310-cp310-linux_`arch`.whl \
        && pip install --upgrade paddlehub -i https://mirror.baidu.com/pypi/simple \
        && pip install -r requirements.txt -i https://mirror.baidu.com/pypi/simple \
#        && pip install protobuf==3.20.3 -i https://mirror.baidu.com/pypi/simple \
        && wget --no-check-certificate -O /PaddleOCR/inference/$DET https://paddleocr.bj.bcebos.com/PP-OCRv4/chinese/$DET \
        && tar xf /PaddleOCR/inference/$DET -C /PaddleOCR/inference/ && rm -rf /PaddleOCR/inference/$DET \
        && wget --no-check-certificate -O /PaddleOCR/inference/$CLS https://paddleocr.bj.bcebos.com/dygraph_v2.0/ch/ch_ppocr_mobile_v2.0_cls_slim_infer.tar \
        && tar xf /PaddleOCR/inference/$CLS -C /PaddleOCR/inference/ && rm -rf /PaddleOCR/inference/CLS \
        && wget --no-check-certificate -O /PaddleOCR/inference/$REC https://paddleocr.bj.bcebos.com/PP-OCRv4/chinese/$REC \
        && tar xf /PaddleOCR/inference/$REC -C /PaddleOCR/inference/ && rm -rf /PaddleOCR/inference/$REC \
        && apt-get update -y && apt-get install ffmpeg libsm6 libxext6  -y \
        && apt-get clean && rm -rf /var/cache/apt && rm -rf /PaddleOCR/.git \
        && sed -i 's|ch_PP-OCRv3_det_infer|ch_PP-OCRv4_det_server_infer|g' /PaddleOCR/deploy/hubserving/ocr_system/params.py \
        && sed -i 's|ch_PP-OCRv3_rec_infer|ch_PP-OCRv4_rec_server_infer|g' /PaddleOCR/deploy/hubserving/ocr_system/params.py \
        && sed -i 's|ch_ppocr_mobile_v2.0_cls_infer|ch_ppocr_mobile_v2.0_cls_slim_infer|g' /PaddleOCR/deploy/hubserving/ocr_system/params.py

WORKDIR /PaddleOCR
EXPOSE 8866

CMD ["/bin/bash","-c","hub install /PaddleOCR/deploy/hubserving/ocr_system/ && hub serving start -m ocr_system"]
