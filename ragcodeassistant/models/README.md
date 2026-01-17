# ONNX Models Directory

This directory is for storing ONNX transformer models for better embedding generation.

## Setup Instructions

### Option 1: Download Pre-converted Model
1. Download a pre-converted ONNX model (e.g., sentence-transformers/all-MiniLM-L6-v2)
2. Place the `.onnx` file here as `sentence-transformers-all-MiniLM-L6-v2.onnx`

### Option 2: Convert from Hugging Face
```bash
# Install required packages
pip install transformers torch onnx

# Convert model to ONNX format
python -c "
from transformers import AutoTokenizer, AutoModel
import torch

model_name = 'sentence-transformers/all-MiniLM-L6-v2'
tokenizer = AutoTokenizer.from_pretrained(model_name)
model = AutoModel.from_pretrained(model_name)

# Export to ONNX
dummy_input = tokenizer('Hello world', return_tensors='pt')
torch.onnx.export(
    model,
    tuple(dummy_input.values()),
    'sentence-transformers-all-MiniLM-L6-v2.onnx',
    input_names=['input_ids', 'attention_mask'],
    output_names=['last_hidden_state'],
    dynamic_axes={'input_ids': {0: 'batch_size', 1: 'sequence'},
                  'attention_mask': {0: 'batch_size', 1: 'sequence'},
                  'last_hidden_state': {0: 'batch_size', 1: 'sequence'}}
)
"
```

## Note
If no ONNX model is present, the application will use stub embeddings that work for testing but provide less accurate semantic similarity.