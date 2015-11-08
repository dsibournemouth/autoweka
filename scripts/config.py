import importlib

def load_config(parser):
    parser.add_argument('--config', required=True)
    args, unknown_args = parser.parse_known_args()
    if args.config.endswith('.py'):
        args.config = args.config.replace('.py', '')
    return importlib.import_module(args.config).__dict__
