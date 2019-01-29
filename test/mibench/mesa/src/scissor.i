# 1 "scissor.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "scissor.c"
# 35 "scissor.c"
# 1 "context.h" 1
# 48 "context.h"
# 1 "types.h" 1
# 90 "types.h"
# 1 "../include/GL/gl.h" 1
# 94 "../include/GL/gl.h"
typedef enum {

 GL_FALSE = 0,
 GL_TRUE = 1,


 GL_BYTE = 0x1400,
 GL_UNSIGNED_BYTE = 0x1401,
 GL_SHORT = 0x1402,
 GL_UNSIGNED_SHORT = 0x1403,
 GL_INT = 0x1404,
 GL_UNSIGNED_INT = 0x1405,
 GL_FLOAT = 0x1406,
 GL_DOUBLE = 0x140A,
 GL_2_BYTES = 0x1407,
 GL_3_BYTES = 0x1408,
 GL_4_BYTES = 0x1409,


 GL_LINES = 0x0001,
 GL_POINTS = 0x0000,
 GL_LINE_STRIP = 0x0003,
 GL_LINE_LOOP = 0x0002,
 GL_TRIANGLES = 0x0004,
 GL_TRIANGLE_STRIP = 0x0005,
 GL_TRIANGLE_FAN = 0x0006,
 GL_QUADS = 0x0007,
 GL_QUAD_STRIP = 0x0008,
 GL_POLYGON = 0x0009,
 GL_EDGE_FLAG = 0x0B43,


 GL_VERTEX_ARRAY = 0x8074,
 GL_NORMAL_ARRAY = 0x8075,
 GL_COLOR_ARRAY = 0x8076,
 GL_INDEX_ARRAY = 0x8077,
 GL_TEXTURE_COORD_ARRAY = 0x8078,
 GL_EDGE_FLAG_ARRAY = 0x8079,
 GL_VERTEX_ARRAY_SIZE = 0x807A,
 GL_VERTEX_ARRAY_TYPE = 0x807B,
 GL_VERTEX_ARRAY_STRIDE = 0x807C,
 GL_NORMAL_ARRAY_TYPE = 0x807E,
 GL_NORMAL_ARRAY_STRIDE = 0x807F,
 GL_COLOR_ARRAY_SIZE = 0x8081,
 GL_COLOR_ARRAY_TYPE = 0x8082,
 GL_COLOR_ARRAY_STRIDE = 0x8083,
 GL_INDEX_ARRAY_TYPE = 0x8085,
 GL_INDEX_ARRAY_STRIDE = 0x8086,
 GL_TEXTURE_COORD_ARRAY_SIZE = 0x8088,
 GL_TEXTURE_COORD_ARRAY_TYPE = 0x8089,
 GL_TEXTURE_COORD_ARRAY_STRIDE = 0x808A,
 GL_EDGE_FLAG_ARRAY_STRIDE = 0x808C,
 GL_VERTEX_ARRAY_POINTER = 0x808E,
 GL_NORMAL_ARRAY_POINTER = 0x808F,
 GL_COLOR_ARRAY_POINTER = 0x8090,
 GL_INDEX_ARRAY_POINTER = 0x8091,
 GL_TEXTURE_COORD_ARRAY_POINTER = 0x8092,
 GL_EDGE_FLAG_ARRAY_POINTER = 0x8093,
        GL_V2F = 0x2A20,
 GL_V3F = 0x2A21,
 GL_C4UB_V2F = 0x2A22,
 GL_C4UB_V3F = 0x2A23,
 GL_C3F_V3F = 0x2A24,
 GL_N3F_V3F = 0x2A25,
 GL_C4F_N3F_V3F = 0x2A26,
 GL_T2F_V3F = 0x2A27,
 GL_T4F_V4F = 0x2A28,
 GL_T2F_C4UB_V3F = 0x2A29,
 GL_T2F_C3F_V3F = 0x2A2A,
 GL_T2F_N3F_V3F = 0x2A2B,
 GL_T2F_C4F_N3F_V3F = 0x2A2C,
 GL_T4F_C4F_N3F_V4F = 0x2A2D,


 GL_MATRIX_MODE = 0x0BA0,
 GL_MODELVIEW = 0x1700,
 GL_PROJECTION = 0x1701,
 GL_TEXTURE = 0x1702,


 GL_POINT_SMOOTH = 0x0B10,
 GL_POINT_SIZE = 0x0B11,
 GL_POINT_SIZE_GRANULARITY = 0x0B13,
 GL_POINT_SIZE_RANGE = 0x0B12,


 GL_LINE_SMOOTH = 0x0B20,
 GL_LINE_STIPPLE = 0x0B24,
 GL_LINE_STIPPLE_PATTERN = 0x0B25,
 GL_LINE_STIPPLE_REPEAT = 0x0B26,
 GL_LINE_WIDTH = 0x0B21,
 GL_LINE_WIDTH_GRANULARITY = 0x0B23,
 GL_LINE_WIDTH_RANGE = 0x0B22,


 GL_POINT = 0x1B00,
 GL_LINE = 0x1B01,
 GL_FILL = 0x1B02,
 GL_CCW = 0x0901,
 GL_CW = 0x0900,
 GL_FRONT = 0x0404,
 GL_BACK = 0x0405,
 GL_CULL_FACE = 0x0B44,
 GL_CULL_FACE_MODE = 0x0B45,
 GL_POLYGON_SMOOTH = 0x0B41,
 GL_POLYGON_STIPPLE = 0x0B42,
 GL_FRONT_FACE = 0x0B46,
 GL_POLYGON_MODE = 0x0B40,
 GL_POLYGON_OFFSET_FACTOR = 0x8038,
 GL_POLYGON_OFFSET_UNITS = 0x2A00,
 GL_POLYGON_OFFSET_POINT = 0x2A01,
 GL_POLYGON_OFFSET_LINE = 0x2A02,
 GL_POLYGON_OFFSET_FILL = 0x8037,


 GL_COMPILE = 0x1300,
 GL_COMPILE_AND_EXECUTE = 0x1301,
 GL_LIST_BASE = 0x0B32,
 GL_LIST_INDEX = 0x0B33,
 GL_LIST_MODE = 0x0B30,


 GL_NEVER = 0x0200,
 GL_LESS = 0x0201,
 GL_GEQUAL = 0x0206,
 GL_LEQUAL = 0x0203,
 GL_GREATER = 0x0204,
 GL_NOTEQUAL = 0x0205,
 GL_EQUAL = 0x0202,
 GL_ALWAYS = 0x0207,
 GL_DEPTH_TEST = 0x0B71,
 GL_DEPTH_BITS = 0x0D56,
 GL_DEPTH_CLEAR_VALUE = 0x0B73,
 GL_DEPTH_FUNC = 0x0B74,
 GL_DEPTH_RANGE = 0x0B70,
 GL_DEPTH_WRITEMASK = 0x0B72,
 GL_DEPTH_COMPONENT = 0x1902,


 GL_LIGHTING = 0x0B50,
 GL_LIGHT0 = 0x4000,
 GL_LIGHT1 = 0x4001,
 GL_LIGHT2 = 0x4002,
 GL_LIGHT3 = 0x4003,
 GL_LIGHT4 = 0x4004,
 GL_LIGHT5 = 0x4005,
 GL_LIGHT6 = 0x4006,
 GL_LIGHT7 = 0x4007,
 GL_SPOT_EXPONENT = 0x1205,
 GL_SPOT_CUTOFF = 0x1206,
 GL_CONSTANT_ATTENUATION = 0x1207,
 GL_LINEAR_ATTENUATION = 0x1208,
 GL_QUADRATIC_ATTENUATION = 0x1209,
 GL_AMBIENT = 0x1200,
 GL_DIFFUSE = 0x1201,
 GL_SPECULAR = 0x1202,
 GL_SHININESS = 0x1601,
 GL_EMISSION = 0x1600,
 GL_POSITION = 0x1203,
 GL_SPOT_DIRECTION = 0x1204,
 GL_AMBIENT_AND_DIFFUSE = 0x1602,
 GL_COLOR_INDEXES = 0x1603,
 GL_LIGHT_MODEL_TWO_SIDE = 0x0B52,
 GL_LIGHT_MODEL_LOCAL_VIEWER = 0x0B51,
 GL_LIGHT_MODEL_AMBIENT = 0x0B53,
 GL_FRONT_AND_BACK = 0x0408,
 GL_SHADE_MODEL = 0x0B54,
 GL_FLAT = 0x1D00,
 GL_SMOOTH = 0x1D01,
 GL_COLOR_MATERIAL = 0x0B57,
 GL_COLOR_MATERIAL_FACE = 0x0B55,
 GL_COLOR_MATERIAL_PARAMETER = 0x0B56,
 GL_NORMALIZE = 0x0BA1,


 GL_CLIP_PLANE0 = 0x3000,
 GL_CLIP_PLANE1 = 0x3001,
 GL_CLIP_PLANE2 = 0x3002,
 GL_CLIP_PLANE3 = 0x3003,
 GL_CLIP_PLANE4 = 0x3004,
 GL_CLIP_PLANE5 = 0x3005,


 GL_ACCUM_RED_BITS = 0x0D58,
 GL_ACCUM_GREEN_BITS = 0x0D59,
 GL_ACCUM_BLUE_BITS = 0x0D5A,
 GL_ACCUM_ALPHA_BITS = 0x0D5B,
 GL_ACCUM_CLEAR_VALUE = 0x0B80,
 GL_ACCUM = 0x0100,
 GL_ADD = 0x0104,
 GL_LOAD = 0x0101,
 GL_MULT = 0x0103,
 GL_RETURN = 0x0102,


 GL_ALPHA_TEST = 0x0BC0,
 GL_ALPHA_TEST_REF = 0x0BC2,
 GL_ALPHA_TEST_FUNC = 0x0BC1,


 GL_BLEND = 0x0BE2,
 GL_BLEND_SRC = 0x0BE1,
 GL_BLEND_DST = 0x0BE0,
 GL_ZERO = 0,
 GL_ONE = 1,
 GL_SRC_COLOR = 0x0300,
 GL_ONE_MINUS_SRC_COLOR = 0x0301,
 GL_DST_COLOR = 0x0306,
 GL_ONE_MINUS_DST_COLOR = 0x0307,
 GL_SRC_ALPHA = 0x0302,
 GL_ONE_MINUS_SRC_ALPHA = 0x0303,
 GL_DST_ALPHA = 0x0304,
 GL_ONE_MINUS_DST_ALPHA = 0x0305,
 GL_SRC_ALPHA_SATURATE = 0x0308,
 GL_CONSTANT_COLOR = 0x8001,
 GL_ONE_MINUS_CONSTANT_COLOR = 0x8002,
 GL_CONSTANT_ALPHA = 0x8003,
 GL_ONE_MINUS_CONSTANT_ALPHA = 0x8004,


 GL_FEEDBACK = 0x1C01,
 GL_RENDER = 0x1C00,
 GL_SELECT = 0x1C02,


 GL_2D = 0x0600,
 GL_3D = 0x0601,
 GL_3D_COLOR = 0x0602,
 GL_3D_COLOR_TEXTURE = 0x0603,
 GL_4D_COLOR_TEXTURE = 0x0604,
 GL_POINT_TOKEN = 0x0701,
 GL_LINE_TOKEN = 0x0702,
 GL_LINE_RESET_TOKEN = 0x0707,
 GL_POLYGON_TOKEN = 0x0703,
 GL_BITMAP_TOKEN = 0x0704,
 GL_DRAW_PIXEL_TOKEN = 0x0705,
 GL_COPY_PIXEL_TOKEN = 0x0706,
 GL_PASS_THROUGH_TOKEN = 0x0700,
 GL_FEEDBACK_BUFFER_POINTER = 0x0DF0,
 GL_FEEDBACK_BUFFER_SIZE = 0x0DF1,
 GL_FEEDBACK_BUFFER_TYPE = 0x0DF2,



 GL_FOG = 0x0B60,
 GL_FOG_MODE = 0x0B65,
 GL_FOG_DENSITY = 0x0B62,
 GL_FOG_COLOR = 0x0B66,
 GL_FOG_INDEX = 0x0B61,
 GL_FOG_START = 0x0B63,
 GL_FOG_END = 0x0B64,
 GL_LINEAR = 0x2601,
 GL_EXP = 0x0800,
 GL_EXP2 = 0x0801,


 GL_LOGIC_OP = 0x0BF1,
 GL_INDEX_LOGIC_OP = 0x0BF1,
 GL_COLOR_LOGIC_OP = 0x0BF2,
 GL_LOGIC_OP_MODE = 0x0BF0,
 GL_CLEAR = 0x1500,
 GL_SET = 0x150F,
 GL_COPY = 0x1503,
 GL_COPY_INVERTED = 0x150C,
 GL_NOOP = 0x1505,
 GL_INVERT = 0x150A,
 GL_AND = 0x1501,
 GL_NAND = 0x150E,
 GL_OR = 0x1507,
 GL_NOR = 0x1508,
 GL_XOR = 0x1506,
 GL_EQUIV = 0x1509,
 GL_AND_REVERSE = 0x1502,
 GL_AND_INVERTED = 0x1504,
 GL_OR_REVERSE = 0x150B,
 GL_OR_INVERTED = 0x150D,


 GL_STENCIL_TEST = 0x0B90,
 GL_STENCIL_WRITEMASK = 0x0B98,
 GL_STENCIL_BITS = 0x0D57,
 GL_STENCIL_FUNC = 0x0B92,
 GL_STENCIL_VALUE_MASK = 0x0B93,
 GL_STENCIL_REF = 0x0B97,
 GL_STENCIL_FAIL = 0x0B94,
 GL_STENCIL_PASS_DEPTH_PASS = 0x0B96,
 GL_STENCIL_PASS_DEPTH_FAIL = 0x0B95,
 GL_STENCIL_CLEAR_VALUE = 0x0B91,
 GL_STENCIL_INDEX = 0x1901,
 GL_KEEP = 0x1E00,
 GL_REPLACE = 0x1E01,
 GL_INCR = 0x1E02,
 GL_DECR = 0x1E03,


 GL_NONE = 0,
 GL_LEFT = 0x0406,
 GL_RIGHT = 0x0407,



 GL_FRONT_LEFT = 0x0400,
 GL_FRONT_RIGHT = 0x0401,
 GL_BACK_LEFT = 0x0402,
 GL_BACK_RIGHT = 0x0403,
 GL_AUX0 = 0x0409,
 GL_AUX1 = 0x040A,
 GL_AUX2 = 0x040B,
 GL_AUX3 = 0x040C,
 GL_COLOR_INDEX = 0x1900,
 GL_RED = 0x1903,
 GL_GREEN = 0x1904,
 GL_BLUE = 0x1905,
 GL_ALPHA = 0x1906,
 GL_LUMINANCE = 0x1909,
 GL_LUMINANCE_ALPHA = 0x190A,
 GL_ALPHA_BITS = 0x0D55,
 GL_RED_BITS = 0x0D52,
 GL_GREEN_BITS = 0x0D53,
 GL_BLUE_BITS = 0x0D54,
 GL_INDEX_BITS = 0x0D51,
 GL_SUBPIXEL_BITS = 0x0D50,
 GL_AUX_BUFFERS = 0x0C00,
 GL_READ_BUFFER = 0x0C02,
 GL_DRAW_BUFFER = 0x0C01,
 GL_DOUBLEBUFFER = 0x0C32,
 GL_STEREO = 0x0C33,
 GL_BITMAP = 0x1A00,
 GL_COLOR = 0x1800,
 GL_DEPTH = 0x1801,
 GL_STENCIL = 0x1802,
 GL_DITHER = 0x0BD0,
 GL_RGB = 0x1907,
 GL_RGBA = 0x1908,


 GL_MAX_LIST_NESTING = 0x0B31,
 GL_MAX_ATTRIB_STACK_DEPTH = 0x0D35,
 GL_MAX_MODELVIEW_STACK_DEPTH = 0x0D36,
 GL_MAX_NAME_STACK_DEPTH = 0x0D37,
 GL_MAX_PROJECTION_STACK_DEPTH = 0x0D38,
 GL_MAX_TEXTURE_STACK_DEPTH = 0x0D39,
 GL_MAX_EVAL_ORDER = 0x0D30,
 GL_MAX_LIGHTS = 0x0D31,
 GL_MAX_CLIP_PLANES = 0x0D32,
 GL_MAX_TEXTURE_SIZE = 0x0D33,
 GL_MAX_PIXEL_MAP_TABLE = 0x0D34,
 GL_MAX_VIEWPORT_DIMS = 0x0D3A,
 GL_MAX_CLIENT_ATTRIB_STACK_DEPTH= 0x0D3B,


 GL_ATTRIB_STACK_DEPTH = 0x0BB0,
 GL_CLIENT_ATTRIB_STACK_DEPTH = 0x0BB1,
 GL_COLOR_CLEAR_VALUE = 0x0C22,
 GL_COLOR_WRITEMASK = 0x0C23,
 GL_CURRENT_INDEX = 0x0B01,
 GL_CURRENT_COLOR = 0x0B00,
 GL_CURRENT_NORMAL = 0x0B02,
 GL_CURRENT_RASTER_COLOR = 0x0B04,
 GL_CURRENT_RASTER_DISTANCE = 0x0B09,
 GL_CURRENT_RASTER_INDEX = 0x0B05,
 GL_CURRENT_RASTER_POSITION = 0x0B07,
 GL_CURRENT_RASTER_TEXTURE_COORDS = 0x0B06,
 GL_CURRENT_RASTER_POSITION_VALID = 0x0B08,
 GL_CURRENT_TEXTURE_COORDS = 0x0B03,
 GL_INDEX_CLEAR_VALUE = 0x0C20,
 GL_INDEX_MODE = 0x0C30,
 GL_INDEX_WRITEMASK = 0x0C21,
 GL_MODELVIEW_MATRIX = 0x0BA6,
 GL_MODELVIEW_STACK_DEPTH = 0x0BA3,
 GL_NAME_STACK_DEPTH = 0x0D70,
 GL_PROJECTION_MATRIX = 0x0BA7,
 GL_PROJECTION_STACK_DEPTH = 0x0BA4,
 GL_RENDER_MODE = 0x0C40,
 GL_RGBA_MODE = 0x0C31,
 GL_TEXTURE_MATRIX = 0x0BA8,
 GL_TEXTURE_STACK_DEPTH = 0x0BA5,
 GL_VIEWPORT = 0x0BA2,



 GL_AUTO_NORMAL = 0x0D80,
 GL_MAP1_COLOR_4 = 0x0D90,
 GL_MAP1_GRID_DOMAIN = 0x0DD0,
 GL_MAP1_GRID_SEGMENTS = 0x0DD1,
 GL_MAP1_INDEX = 0x0D91,
 GL_MAP1_NORMAL = 0x0D92,
 GL_MAP1_TEXTURE_COORD_1 = 0x0D93,
 GL_MAP1_TEXTURE_COORD_2 = 0x0D94,
 GL_MAP1_TEXTURE_COORD_3 = 0x0D95,
 GL_MAP1_TEXTURE_COORD_4 = 0x0D96,
 GL_MAP1_VERTEX_3 = 0x0D97,
 GL_MAP1_VERTEX_4 = 0x0D98,
 GL_MAP2_COLOR_4 = 0x0DB0,
 GL_MAP2_GRID_DOMAIN = 0x0DD2,
 GL_MAP2_GRID_SEGMENTS = 0x0DD3,
 GL_MAP2_INDEX = 0x0DB1,
 GL_MAP2_NORMAL = 0x0DB2,
 GL_MAP2_TEXTURE_COORD_1 = 0x0DB3,
 GL_MAP2_TEXTURE_COORD_2 = 0x0DB4,
 GL_MAP2_TEXTURE_COORD_3 = 0x0DB5,
 GL_MAP2_TEXTURE_COORD_4 = 0x0DB6,
 GL_MAP2_VERTEX_3 = 0x0DB7,
 GL_MAP2_VERTEX_4 = 0x0DB8,
 GL_COEFF = 0x0A00,
 GL_DOMAIN = 0x0A02,
 GL_ORDER = 0x0A01,


 GL_FOG_HINT = 0x0C54,
 GL_LINE_SMOOTH_HINT = 0x0C52,
 GL_PERSPECTIVE_CORRECTION_HINT = 0x0C50,
 GL_POINT_SMOOTH_HINT = 0x0C51,
 GL_POLYGON_SMOOTH_HINT = 0x0C53,
 GL_DONT_CARE = 0x1100,
 GL_FASTEST = 0x1101,
 GL_NICEST = 0x1102,


 GL_SCISSOR_TEST = 0x0C11,
 GL_SCISSOR_BOX = 0x0C10,


 GL_MAP_COLOR = 0x0D10,
 GL_MAP_STENCIL = 0x0D11,
 GL_INDEX_SHIFT = 0x0D12,
 GL_INDEX_OFFSET = 0x0D13,
 GL_RED_SCALE = 0x0D14,
 GL_RED_BIAS = 0x0D15,
 GL_GREEN_SCALE = 0x0D18,
 GL_GREEN_BIAS = 0x0D19,
 GL_BLUE_SCALE = 0x0D1A,
 GL_BLUE_BIAS = 0x0D1B,
 GL_ALPHA_SCALE = 0x0D1C,
 GL_ALPHA_BIAS = 0x0D1D,
 GL_DEPTH_SCALE = 0x0D1E,
 GL_DEPTH_BIAS = 0x0D1F,
 GL_PIXEL_MAP_S_TO_S_SIZE = 0x0CB1,
 GL_PIXEL_MAP_I_TO_I_SIZE = 0x0CB0,
 GL_PIXEL_MAP_I_TO_R_SIZE = 0x0CB2,
 GL_PIXEL_MAP_I_TO_G_SIZE = 0x0CB3,
 GL_PIXEL_MAP_I_TO_B_SIZE = 0x0CB4,
 GL_PIXEL_MAP_I_TO_A_SIZE = 0x0CB5,
 GL_PIXEL_MAP_R_TO_R_SIZE = 0x0CB6,
 GL_PIXEL_MAP_G_TO_G_SIZE = 0x0CB7,
 GL_PIXEL_MAP_B_TO_B_SIZE = 0x0CB8,
 GL_PIXEL_MAP_A_TO_A_SIZE = 0x0CB9,
 GL_PIXEL_MAP_S_TO_S = 0x0C71,
 GL_PIXEL_MAP_I_TO_I = 0x0C70,
 GL_PIXEL_MAP_I_TO_R = 0x0C72,
 GL_PIXEL_MAP_I_TO_G = 0x0C73,
 GL_PIXEL_MAP_I_TO_B = 0x0C74,
 GL_PIXEL_MAP_I_TO_A = 0x0C75,
 GL_PIXEL_MAP_R_TO_R = 0x0C76,
 GL_PIXEL_MAP_G_TO_G = 0x0C77,
 GL_PIXEL_MAP_B_TO_B = 0x0C78,
 GL_PIXEL_MAP_A_TO_A = 0x0C79,
 GL_PACK_ALIGNMENT = 0x0D05,
 GL_PACK_LSB_FIRST = 0x0D01,
 GL_PACK_ROW_LENGTH = 0x0D02,
 GL_PACK_SKIP_PIXELS = 0x0D04,
 GL_PACK_SKIP_ROWS = 0x0D03,
 GL_PACK_SWAP_BYTES = 0x0D00,
 GL_UNPACK_ALIGNMENT = 0x0CF5,
 GL_UNPACK_LSB_FIRST = 0x0CF1,
 GL_UNPACK_ROW_LENGTH = 0x0CF2,
 GL_UNPACK_SKIP_PIXELS = 0x0CF4,
 GL_UNPACK_SKIP_ROWS = 0x0CF3,
 GL_UNPACK_SWAP_BYTES = 0x0CF0,
 GL_ZOOM_X = 0x0D16,
 GL_ZOOM_Y = 0x0D17,


 GL_TEXTURE_ENV = 0x2300,
 GL_TEXTURE_ENV_MODE = 0x2200,
 GL_TEXTURE_1D = 0x0DE0,
 GL_TEXTURE_2D = 0x0DE1,
 GL_TEXTURE_WRAP_S = 0x2802,
 GL_TEXTURE_WRAP_T = 0x2803,
 GL_TEXTURE_MAG_FILTER = 0x2800,
 GL_TEXTURE_MIN_FILTER = 0x2801,
 GL_TEXTURE_ENV_COLOR = 0x2201,
 GL_TEXTURE_GEN_S = 0x0C60,
 GL_TEXTURE_GEN_T = 0x0C61,
 GL_TEXTURE_GEN_MODE = 0x2500,
 GL_TEXTURE_BORDER_COLOR = 0x1004,
 GL_TEXTURE_WIDTH = 0x1000,
 GL_TEXTURE_HEIGHT = 0x1001,
 GL_TEXTURE_BORDER = 0x1005,
 GL_TEXTURE_COMPONENTS = 0x1003,
 GL_TEXTURE_RED_SIZE = 0x805C,
 GL_TEXTURE_GREEN_SIZE = 0x805D,
 GL_TEXTURE_BLUE_SIZE = 0x805E,
 GL_TEXTURE_ALPHA_SIZE = 0x805F,
 GL_TEXTURE_LUMINANCE_SIZE = 0x8060,
 GL_TEXTURE_INTENSITY_SIZE = 0x8061,
 GL_NEAREST_MIPMAP_NEAREST = 0x2700,
 GL_NEAREST_MIPMAP_LINEAR = 0x2702,
 GL_LINEAR_MIPMAP_NEAREST = 0x2701,
 GL_LINEAR_MIPMAP_LINEAR = 0x2703,
 GL_OBJECT_LINEAR = 0x2401,
 GL_OBJECT_PLANE = 0x2501,
 GL_EYE_LINEAR = 0x2400,
 GL_EYE_PLANE = 0x2502,
 GL_SPHERE_MAP = 0x2402,
 GL_DECAL = 0x2101,
 GL_MODULATE = 0x2100,
 GL_NEAREST = 0x2600,
 GL_REPEAT = 0x2901,
 GL_CLAMP = 0x2900,
 GL_S = 0x2000,
 GL_T = 0x2001,
 GL_R = 0x2002,
 GL_Q = 0x2003,
 GL_TEXTURE_GEN_R = 0x0C62,
 GL_TEXTURE_GEN_Q = 0x0C63,

 GL_PROXY_TEXTURE_1D = 0x8063,
 GL_PROXY_TEXTURE_2D = 0x8064,
 GL_TEXTURE_PRIORITY = 0x8066,
 GL_TEXTURE_RESIDENT = 0x8067,
 GL_TEXTURE_BINDING_1D = 0x8068,
 GL_TEXTURE_BINDING_2D = 0x8069,


 GL_ALPHA4 = 0x803B,
 GL_ALPHA8 = 0x803C,
 GL_ALPHA12 = 0x803D,
 GL_ALPHA16 = 0x803E,
 GL_LUMINANCE4 = 0x803F,
 GL_LUMINANCE8 = 0x8040,
 GL_LUMINANCE12 = 0x8041,
 GL_LUMINANCE16 = 0x8042,
 GL_LUMINANCE4_ALPHA4 = 0x8043,
 GL_LUMINANCE6_ALPHA2 = 0x8044,
 GL_LUMINANCE8_ALPHA8 = 0x8045,
 GL_LUMINANCE12_ALPHA4 = 0x8046,
 GL_LUMINANCE12_ALPHA12 = 0x8047,
 GL_LUMINANCE16_ALPHA16 = 0x8048,
 GL_INTENSITY = 0x8049,
 GL_INTENSITY4 = 0x804A,
 GL_INTENSITY8 = 0x804B,
 GL_INTENSITY12 = 0x804C,
 GL_INTENSITY16 = 0x804D,
 GL_R3_G3_B2 = 0x2A10,
 GL_RGB4 = 0x804F,
 GL_RGB5 = 0x8050,
 GL_RGB8 = 0x8051,
 GL_RGB10 = 0x8052,
 GL_RGB12 = 0x8053,
 GL_RGB16 = 0x8054,
 GL_RGBA2 = 0x8055,
 GL_RGBA4 = 0x8056,
 GL_RGB5_A1 = 0x8057,
 GL_RGBA8 = 0x8058,
 GL_RGB10_A2 = 0x8059,
 GL_RGBA12 = 0x805A,
 GL_RGBA16 = 0x805B,


 GL_VENDOR = 0x1F00,
 GL_RENDERER = 0x1F01,
 GL_VERSION = 0x1F02,
 GL_EXTENSIONS = 0x1F03,


 GL_INVALID_VALUE = 0x0501,
 GL_INVALID_ENUM = 0x0500,
 GL_INVALID_OPERATION = 0x0502,
 GL_STACK_OVERFLOW = 0x0503,
 GL_STACK_UNDERFLOW = 0x0504,
 GL_OUT_OF_MEMORY = 0x0505,





 GL_CONSTANT_COLOR_EXT = 0x8001,
 GL_ONE_MINUS_CONSTANT_COLOR_EXT = 0x8002,
 GL_CONSTANT_ALPHA_EXT = 0x8003,
 GL_ONE_MINUS_CONSTANT_ALPHA_EXT = 0x8004,
 GL_BLEND_EQUATION_EXT = 0x8009,
 GL_MIN_EXT = 0x8007,
 GL_MAX_EXT = 0x8008,
 GL_FUNC_ADD_EXT = 0x8006,
 GL_FUNC_SUBTRACT_EXT = 0x800A,
 GL_FUNC_REVERSE_SUBTRACT_EXT = 0x800B,
 GL_BLEND_COLOR_EXT = 0x8005,


        GL_POLYGON_OFFSET_EXT = 0x8037,
        GL_POLYGON_OFFSET_FACTOR_EXT = 0x8038,
        GL_POLYGON_OFFSET_BIAS_EXT = 0x8039,


 GL_VERTEX_ARRAY_EXT = 0x8074,
 GL_NORMAL_ARRAY_EXT = 0x8075,
 GL_COLOR_ARRAY_EXT = 0x8076,
 GL_INDEX_ARRAY_EXT = 0x8077,
 GL_TEXTURE_COORD_ARRAY_EXT = 0x8078,
 GL_EDGE_FLAG_ARRAY_EXT = 0x8079,
 GL_VERTEX_ARRAY_SIZE_EXT = 0x807A,
 GL_VERTEX_ARRAY_TYPE_EXT = 0x807B,
 GL_VERTEX_ARRAY_STRIDE_EXT = 0x807C,
 GL_VERTEX_ARRAY_COUNT_EXT = 0x807D,
 GL_NORMAL_ARRAY_TYPE_EXT = 0x807E,
 GL_NORMAL_ARRAY_STRIDE_EXT = 0x807F,
 GL_NORMAL_ARRAY_COUNT_EXT = 0x8080,
 GL_COLOR_ARRAY_SIZE_EXT = 0x8081,
 GL_COLOR_ARRAY_TYPE_EXT = 0x8082,
 GL_COLOR_ARRAY_STRIDE_EXT = 0x8083,
 GL_COLOR_ARRAY_COUNT_EXT = 0x8084,
 GL_INDEX_ARRAY_TYPE_EXT = 0x8085,
 GL_INDEX_ARRAY_STRIDE_EXT = 0x8086,
 GL_INDEX_ARRAY_COUNT_EXT = 0x8087,
 GL_TEXTURE_COORD_ARRAY_SIZE_EXT = 0x8088,
 GL_TEXTURE_COORD_ARRAY_TYPE_EXT = 0x8089,
 GL_TEXTURE_COORD_ARRAY_STRIDE_EXT= 0x808A,
 GL_TEXTURE_COORD_ARRAY_COUNT_EXT= 0x808B,
 GL_EDGE_FLAG_ARRAY_STRIDE_EXT = 0x808C,
 GL_EDGE_FLAG_ARRAY_COUNT_EXT = 0x808D,
 GL_VERTEX_ARRAY_POINTER_EXT = 0x808E,
 GL_NORMAL_ARRAY_POINTER_EXT = 0x808F,
 GL_COLOR_ARRAY_POINTER_EXT = 0x8090,
 GL_INDEX_ARRAY_POINTER_EXT = 0x8091,
 GL_TEXTURE_COORD_ARRAY_POINTER_EXT= 0x8092,
 GL_EDGE_FLAG_ARRAY_POINTER_EXT = 0x8093,


 GL_TEXTURE_PRIORITY_EXT = 0x8066,
 GL_TEXTURE_RESIDENT_EXT = 0x8067,
 GL_TEXTURE_1D_BINDING_EXT = 0x8068,
 GL_TEXTURE_2D_BINDING_EXT = 0x8069,


 GL_PACK_SKIP_IMAGES_EXT = 0x806B,
 GL_PACK_IMAGE_HEIGHT_EXT = 0x806C,
 GL_UNPACK_SKIP_IMAGES_EXT = 0x806D,
 GL_UNPACK_IMAGE_HEIGHT_EXT = 0x806E,
 GL_TEXTURE_3D_EXT = 0x806F,
 GL_PROXY_TEXTURE_3D_EXT = 0x8070,
 GL_TEXTURE_DEPTH_EXT = 0x8071,
 GL_TEXTURE_WRAP_R_EXT = 0x8072,
 GL_MAX_3D_TEXTURE_SIZE_EXT = 0x8073,
 GL_TEXTURE_3D_BINDING_EXT = 0x806A

}






  GLenum;
# 756 "../include/GL/gl.h"
enum {
 GL_CURRENT_BIT = 0x00000001,
 GL_POINT_BIT = 0x00000002,
 GL_LINE_BIT = 0x00000004,
 GL_POLYGON_BIT = 0x00000008,
 GL_POLYGON_STIPPLE_BIT = 0x00000010,
 GL_PIXEL_MODE_BIT = 0x00000020,
 GL_LIGHTING_BIT = 0x00000040,
 GL_FOG_BIT = 0x00000080,
 GL_DEPTH_BUFFER_BIT = 0x00000100,
 GL_ACCUM_BUFFER_BIT = 0x00000200,
 GL_STENCIL_BUFFER_BIT = 0x00000400,
 GL_VIEWPORT_BIT = 0x00000800,
 GL_TRANSFORM_BIT = 0x00001000,
 GL_ENABLE_BIT = 0x00002000,
 GL_COLOR_BUFFER_BIT = 0x00004000,
 GL_HINT_BIT = 0x00008000,
 GL_EVAL_BIT = 0x00010000,
 GL_LIST_BIT = 0x00020000,
 GL_TEXTURE_BIT = 0x00040000,
 GL_SCISSOR_BIT = 0x00080000,
 GL_ALL_ATTRIB_BITS = 0x000fffff
};


enum {
 GL_CLIENT_PIXEL_STORE_BIT = 0x00000001,
 GL_CLIENT_VERTEX_ARRAY_BIT = 0x00000002,
 GL_CLIENT_ALL_ATTRIB_BITS = 0x0000FFFF
};



typedef unsigned int GLbitfield;
# 805 "../include/GL/gl.h"
typedef void GLvoid;
typedef unsigned char GLboolean;
typedef signed char GLbyte;
typedef short GLshort;
typedef int GLint;
typedef unsigned char GLubyte;
typedef unsigned short GLushort;
typedef unsigned int GLuint;
typedef int GLsizei;
typedef float GLfloat;
typedef float GLclampf;
typedef double GLdouble;
typedef double GLclampd;
# 830 "../include/GL/gl.h"
extern void glClearIndex( GLfloat c );

extern void glClearColor( GLclampf red,
     GLclampf green,
     GLclampf blue,
     GLclampf alpha );

extern void glClear( GLbitfield mask );

extern void glIndexMask( GLuint mask );

extern void glColorMask( GLboolean red, GLboolean green,
    GLboolean blue, GLboolean alpha );

extern void glAlphaFunc( GLenum func, GLclampf ref );

extern void glBlendFunc( GLenum sfactor, GLenum dfactor );

extern void glLogicOp( GLenum opcode );

extern void glCullFace( GLenum mode );

extern void glFrontFace( GLenum mode );

extern void glPointSize( GLfloat size );

extern void glLineWidth( GLfloat width );

extern void glLineStipple( GLint factor, GLushort pattern );

extern void glPolygonMode( GLenum face, GLenum mode );

extern void glPolygonOffset( GLfloat factor, GLfloat units );

extern void glPolygonStipple( const GLubyte *mask );

extern void glGetPolygonStipple( GLubyte *mask );

extern void glEdgeFlag( GLboolean flag );

extern void glEdgeFlagv( const GLboolean *flag );

extern void glScissor( GLint x, GLint y, GLsizei width, GLsizei height);

extern void glClipPlane( GLenum plane, const GLdouble *equation );

extern void glGetClipPlane( GLenum plane, GLdouble *equation );

extern void glDrawBuffer( GLenum mode );

extern void glReadBuffer( GLenum mode );

extern void glEnable( GLenum cap );

extern void glDisable( GLenum cap );

extern GLboolean glIsEnabled( GLenum cap );


extern void glGetBooleanv( GLenum pname, GLboolean *params );

extern void glGetDoublev( GLenum pname, GLdouble *params );

extern void glGetFloatv( GLenum pname, GLfloat *params );

extern void glGetIntegerv( GLenum pname, GLint *params );


extern void glPushAttrib( GLbitfield mask );

extern void glPopAttrib( void );


extern void glPushClientAttrib( GLbitfield mask );

extern void glPopClientAttrib( void );


extern GLint glRenderMode( GLenum mode );

extern GLenum glGetError( void );

extern const GLubyte *glGetString( GLenum name );

extern void glFinish( void );

extern void glFlush( void );

extern void glHint( GLenum target, GLenum mode );







extern void glClearDepth( GLclampd depth );

extern void glDepthFunc( GLenum func );

extern void glDepthMask( GLboolean flag );

extern void glDepthRange( GLclampd near_val, GLclampd far_val );






extern void glClearAccum( GLfloat red, GLfloat green,
     GLfloat blue, GLfloat alpha );

extern void glAccum( GLenum op, GLfloat value );







extern void glMatrixMode( GLenum mode );

extern void glOrtho( GLdouble left, GLdouble right,
       GLdouble bottom, GLdouble top,
       GLdouble near_val, GLdouble far_val );

extern void glFrustum( GLdouble left, GLdouble right,
         GLdouble bottom, GLdouble top,
         GLdouble near_val, GLdouble far_val );

extern void glViewport( GLint x, GLint y, GLsizei width, GLsizei height );

extern void glPushMatrix( void );

extern void glPopMatrix( void );

extern void glLoadIdentity( void );

extern void glLoadMatrixd( const GLdouble *m );
extern void glLoadMatrixf( const GLfloat *m );

extern void glMultMatrixd( const GLdouble *m );
extern void glMultMatrixf( const GLfloat *m );

extern void glRotated( GLdouble angle, GLdouble x, GLdouble y, GLdouble z );
extern void glRotatef( GLfloat angle, GLfloat x, GLfloat y, GLfloat z );

extern void glScaled( GLdouble x, GLdouble y, GLdouble z );
extern void glScalef( GLfloat x, GLfloat y, GLfloat z );

extern void glTranslated( GLdouble x, GLdouble y, GLdouble z );
extern void glTranslatef( GLfloat x, GLfloat y, GLfloat z );







extern GLboolean glIsList( GLuint list );

extern void glDeleteLists( GLuint list, GLsizei range );

extern GLuint glGenLists( GLsizei range );

extern void glNewList( GLuint list, GLenum mode );

extern void glEndList( void );

extern void glCallList( GLuint list );

extern void glCallLists( GLsizei n, GLenum type, const GLvoid *lists );

extern void glListBase( GLuint base );







extern void glBegin( GLenum mode );

extern void glEnd( void );


extern void glVertex2d( GLdouble x, GLdouble y );
extern void glVertex2f( GLfloat x, GLfloat y );
extern void glVertex2i( GLint x, GLint y );
extern void glVertex2s( GLshort x, GLshort y );

extern void glVertex3d( GLdouble x, GLdouble y, GLdouble z );
extern void glVertex3f( GLfloat x, GLfloat y, GLfloat z );
extern void glVertex3i( GLint x, GLint y, GLint z );
extern void glVertex3s( GLshort x, GLshort y, GLshort z );

extern void glVertex4d( GLdouble x, GLdouble y, GLdouble z, GLdouble w );
extern void glVertex4f( GLfloat x, GLfloat y, GLfloat z, GLfloat w );
extern void glVertex4i( GLint x, GLint y, GLint z, GLint w );
extern void glVertex4s( GLshort x, GLshort y, GLshort z, GLshort w );

extern void glVertex2dv( const GLdouble *v );
extern void glVertex2fv( const GLfloat *v );
extern void glVertex2iv( const GLint *v );
extern void glVertex2sv( const GLshort *v );

extern void glVertex3dv( const GLdouble *v );
extern void glVertex3fv( const GLfloat *v );
extern void glVertex3iv( const GLint *v );
extern void glVertex3sv( const GLshort *v );

extern void glVertex4dv( const GLdouble *v );
extern void glVertex4fv( const GLfloat *v );
extern void glVertex4iv( const GLint *v );
extern void glVertex4sv( const GLshort *v );


extern void glNormal3b( GLbyte nx, GLbyte ny, GLbyte nz );
extern void glNormal3d( GLdouble nx, GLdouble ny, GLdouble nz );
extern void glNormal3f( GLfloat nx, GLfloat ny, GLfloat nz );
extern void glNormal3i( GLint nx, GLint ny, GLint nz );
extern void glNormal3s( GLshort nx, GLshort ny, GLshort nz );

extern void glNormal3bv( const GLbyte *v );
extern void glNormal3dv( const GLdouble *v );
extern void glNormal3fv( const GLfloat *v );
extern void glNormal3iv( const GLint *v );
extern void glNormal3sv( const GLshort *v );


extern void glIndexd( GLdouble c );
extern void glIndexf( GLfloat c );
extern void glIndexi( GLint c );
extern void glIndexs( GLshort c );
extern void glIndexub( GLubyte c );

extern void glIndexdv( const GLdouble *c );
extern void glIndexfv( const GLfloat *c );
extern void glIndexiv( const GLint *c );
extern void glIndexsv( const GLshort *c );
extern void glIndexubv( const GLubyte *c );

extern void glColor3b( GLbyte red, GLbyte green, GLbyte blue );
extern void glColor3d( GLdouble red, GLdouble green, GLdouble blue );
extern void glColor3f( GLfloat red, GLfloat green, GLfloat blue );
extern void glColor3i( GLint red, GLint green, GLint blue );
extern void glColor3s( GLshort red, GLshort green, GLshort blue );
extern void glColor3ub( GLubyte red, GLubyte green, GLubyte blue );
extern void glColor3ui( GLuint red, GLuint green, GLuint blue );
extern void glColor3us( GLushort red, GLushort green, GLushort blue );

extern void glColor4b( GLbyte red, GLbyte green, GLbyte blue, GLbyte alpha );
extern void glColor4d( GLdouble red, GLdouble green,
         GLdouble blue, GLdouble alpha );
extern void glColor4f( GLfloat red, GLfloat green,
         GLfloat blue, GLfloat alpha );
extern void glColor4i( GLint red, GLint green, GLint blue, GLint alpha );
extern void glColor4s( GLshort red, GLshort green,
         GLshort blue, GLshort alpha );
extern void glColor4ub( GLubyte red, GLubyte green,
   GLubyte blue, GLubyte alpha );
extern void glColor4ui( GLuint red, GLuint green, GLuint blue, GLuint alpha );
extern void glColor4us( GLushort red, GLushort green,
   GLushort blue, GLushort alpha );


extern void glColor3bv( const GLbyte *v );
extern void glColor3dv( const GLdouble *v );
extern void glColor3fv( const GLfloat *v );
extern void glColor3iv( const GLint *v );
extern void glColor3sv( const GLshort *v );
extern void glColor3ubv( const GLubyte *v );
extern void glColor3uiv( const GLuint *v );
extern void glColor3usv( const GLushort *v );

extern void glColor4bv( const GLbyte *v );
extern void glColor4dv( const GLdouble *v );
extern void glColor4fv( const GLfloat *v );
extern void glColor4iv( const GLint *v );
extern void glColor4sv( const GLshort *v );
extern void glColor4ubv( const GLubyte *v );
extern void glColor4uiv( const GLuint *v );
extern void glColor4usv( const GLushort *v );


extern void glTexCoord1d( GLdouble s );
extern void glTexCoord1f( GLfloat s );
extern void glTexCoord1i( GLint s );
extern void glTexCoord1s( GLshort s );

extern void glTexCoord2d( GLdouble s, GLdouble t );
extern void glTexCoord2f( GLfloat s, GLfloat t );
extern void glTexCoord2i( GLint s, GLint t );
extern void glTexCoord2s( GLshort s, GLshort t );

extern void glTexCoord3d( GLdouble s, GLdouble t, GLdouble r );
extern void glTexCoord3f( GLfloat s, GLfloat t, GLfloat r );
extern void glTexCoord3i( GLint s, GLint t, GLint r );
extern void glTexCoord3s( GLshort s, GLshort t, GLshort r );

extern void glTexCoord4d( GLdouble s, GLdouble t, GLdouble r, GLdouble q );
extern void glTexCoord4f( GLfloat s, GLfloat t, GLfloat r, GLfloat q );
extern void glTexCoord4i( GLint s, GLint t, GLint r, GLint q );
extern void glTexCoord4s( GLshort s, GLshort t, GLshort r, GLshort q );

extern void glTexCoord1dv( const GLdouble *v );
extern void glTexCoord1fv( const GLfloat *v );
extern void glTexCoord1iv( const GLint *v );
extern void glTexCoord1sv( const GLshort *v );

extern void glTexCoord2dv( const GLdouble *v );
extern void glTexCoord2fv( const GLfloat *v );
extern void glTexCoord2iv( const GLint *v );
extern void glTexCoord2sv( const GLshort *v );

extern void glTexCoord3dv( const GLdouble *v );
extern void glTexCoord3fv( const GLfloat *v );
extern void glTexCoord3iv( const GLint *v );
extern void glTexCoord3sv( const GLshort *v );

extern void glTexCoord4dv( const GLdouble *v );
extern void glTexCoord4fv( const GLfloat *v );
extern void glTexCoord4iv( const GLint *v );
extern void glTexCoord4sv( const GLshort *v );


extern void glRasterPos2d( GLdouble x, GLdouble y );
extern void glRasterPos2f( GLfloat x, GLfloat y );
extern void glRasterPos2i( GLint x, GLint y );
extern void glRasterPos2s( GLshort x, GLshort y );

extern void glRasterPos3d( GLdouble x, GLdouble y, GLdouble z );
extern void glRasterPos3f( GLfloat x, GLfloat y, GLfloat z );
extern void glRasterPos3i( GLint x, GLint y, GLint z );
extern void glRasterPos3s( GLshort x, GLshort y, GLshort z );

extern void glRasterPos4d( GLdouble x, GLdouble y, GLdouble z, GLdouble w );
extern void glRasterPos4f( GLfloat x, GLfloat y, GLfloat z, GLfloat w );
extern void glRasterPos4i( GLint x, GLint y, GLint z, GLint w );
extern void glRasterPos4s( GLshort x, GLshort y, GLshort z, GLshort w );

extern void glRasterPos2dv( const GLdouble *v );
extern void glRasterPos2fv( const GLfloat *v );
extern void glRasterPos2iv( const GLint *v );
extern void glRasterPos2sv( const GLshort *v );

extern void glRasterPos3dv( const GLdouble *v );
extern void glRasterPos3fv( const GLfloat *v );
extern void glRasterPos3iv( const GLint *v );
extern void glRasterPos3sv( const GLshort *v );

extern void glRasterPos4dv( const GLdouble *v );
extern void glRasterPos4fv( const GLfloat *v );
extern void glRasterPos4iv( const GLint *v );
extern void glRasterPos4sv( const GLshort *v );


extern void glRectd( GLdouble x1, GLdouble y1, GLdouble x2, GLdouble y2 );
extern void glRectf( GLfloat x1, GLfloat y1, GLfloat x2, GLfloat y2 );
extern void glRecti( GLint x1, GLint y1, GLint x2, GLint y2 );
extern void glRects( GLshort x1, GLshort y1, GLshort x2, GLshort y2 );


extern void glRectdv( const GLdouble *v1, const GLdouble *v2 );
extern void glRectfv( const GLfloat *v1, const GLfloat *v2 );
extern void glRectiv( const GLint *v1, const GLint *v2 );
extern void glRectsv( const GLshort *v1, const GLshort *v2 );







extern void glVertexPointer( GLint size, GLenum type, GLsizei stride,
                             const GLvoid *ptr );

extern void glNormalPointer( GLenum type, GLsizei stride,
                             const GLvoid *ptr );

extern void glColorPointer( GLint size, GLenum type, GLsizei stride,
                            const GLvoid *ptr );

extern void glIndexPointer( GLenum type, GLsizei stride, const GLvoid *ptr );

extern void glTexCoordPointer( GLint size, GLenum type, GLsizei stride,
                               const GLvoid *ptr );

extern void glEdgeFlagPointer( GLsizei stride, const GLboolean *ptr );

extern void glGetPointerv( GLenum pname, void **params );

extern void glArrayElement( GLint i );

extern void glDrawArrays( GLenum mode, GLint first, GLsizei count );

extern void glDrawElements( GLenum mode, GLsizei count,
                            GLenum type, const GLvoid *indices );

extern void glInterleavedArrays( GLenum format, GLsizei stride,
                                 const GLvoid *pointer );






extern void glShadeModel( GLenum mode );

extern void glLightf( GLenum light, GLenum pname, GLfloat param );
extern void glLighti( GLenum light, GLenum pname, GLint param );
extern void glLightfv( GLenum light, GLenum pname, const GLfloat *params );
extern void glLightiv( GLenum light, GLenum pname, const GLint *params );

extern void glGetLightfv( GLenum light, GLenum pname, GLfloat *params );
extern void glGetLightiv( GLenum light, GLenum pname, GLint *params );

extern void glLightModelf( GLenum pname, GLfloat param );
extern void glLightModeli( GLenum pname, GLint param );
extern void glLightModelfv( GLenum pname, const GLfloat *params );
extern void glLightModeliv( GLenum pname, const GLint *params );

extern void glMaterialf( GLenum face, GLenum pname, GLfloat param );
extern void glMateriali( GLenum face, GLenum pname, GLint param );
extern void glMaterialfv( GLenum face, GLenum pname, const GLfloat *params );
extern void glMaterialiv( GLenum face, GLenum pname, const GLint *params );

extern void glGetMaterialfv( GLenum face, GLenum pname, GLfloat *params );
extern void glGetMaterialiv( GLenum face, GLenum pname, GLint *params );

extern void glColorMaterial( GLenum face, GLenum mode );
# 1269 "../include/GL/gl.h"
extern void glPixelZoom( GLfloat xfactor, GLfloat yfactor );

extern void glPixelStoref( GLenum pname, GLfloat param );
extern void glPixelStorei( GLenum pname, GLint param );

extern void glPixelTransferf( GLenum pname, GLfloat param );
extern void glPixelTransferi( GLenum pname, GLint param );

extern void glPixelMapfv( GLenum map, GLint mapsize, const GLfloat *values );
extern void glPixelMapuiv( GLenum map, GLint mapsize, const GLuint *values );
extern void glPixelMapusv( GLenum map, GLint mapsize, const GLushort *values );

extern void glGetPixelMapfv( GLenum map, GLfloat *values );
extern void glGetPixelMapuiv( GLenum map, GLuint *values );
extern void glGetPixelMapusv( GLenum map, GLushort *values );

extern void glBitmap( GLsizei width, GLsizei height,
        GLfloat xorig, GLfloat yorig,
        GLfloat xmove, GLfloat ymove,
        const GLubyte *bitmap );

extern void glReadPixels( GLint x, GLint y, GLsizei width, GLsizei height,
     GLenum format, GLenum type, GLvoid *pixels );

extern void glDrawPixels( GLsizei width, GLsizei height,
     GLenum format, GLenum type, const GLvoid *pixels );

extern void glCopyPixels( GLint x, GLint y, GLsizei width, GLsizei height,
     GLenum type );







extern void glStencilFunc( GLenum func, GLint ref, GLuint mask );

extern void glStencilMask( GLuint mask );

extern void glStencilOp( GLenum fail, GLenum zfail, GLenum zpass );

extern void glClearStencil( GLint s );







extern void glTexGend( GLenum coord, GLenum pname, GLdouble param );
extern void glTexGenf( GLenum coord, GLenum pname, GLfloat param );
extern void glTexGeni( GLenum coord, GLenum pname, GLint param );

extern void glTexGendv( GLenum coord, GLenum pname, const GLdouble *params );
extern void glTexGenfv( GLenum coord, GLenum pname, const GLfloat *params );
extern void glTexGeniv( GLenum coord, GLenum pname, const GLint *params );

extern void glGetTexGendv( GLenum coord, GLenum pname, GLdouble *params );
extern void glGetTexGenfv( GLenum coord, GLenum pname, GLfloat *params );
extern void glGetTexGeniv( GLenum coord, GLenum pname, GLint *params );


extern void glTexEnvf( GLenum target, GLenum pname, GLfloat param );
extern void glTexEnvi( GLenum target, GLenum pname, GLint param );

extern void glTexEnvfv( GLenum target, GLenum pname, const GLfloat *params );
extern void glTexEnviv( GLenum target, GLenum pname, const GLint *params );

extern void glGetTexEnvfv( GLenum target, GLenum pname, GLfloat *params );
extern void glGetTexEnviv( GLenum target, GLenum pname, GLint *params );


extern void glTexParameterf( GLenum target, GLenum pname, GLfloat param );
extern void glTexParameteri( GLenum target, GLenum pname, GLint param );

extern void glTexParameterfv( GLenum target, GLenum pname,
         const GLfloat *params );
extern void glTexParameteriv( GLenum target, GLenum pname,
         const GLint *params );

extern void glGetTexParameterfv( GLenum target, GLenum pname, GLfloat *params);
extern void glGetTexParameteriv( GLenum target, GLenum pname, GLint *params );

extern void glGetTexLevelParameterfv( GLenum target, GLint level,
          GLenum pname, GLfloat *params );
extern void glGetTexLevelParameteriv( GLenum target, GLint level,
          GLenum pname, GLint *params );


extern void glTexImage1D( GLenum target, GLint level, GLint components,
     GLsizei width, GLint border,
     GLenum format, GLenum type, const GLvoid *pixels );

extern void glTexImage2D( GLenum target, GLint level, GLint components,
     GLsizei width, GLsizei height, GLint border,
     GLenum format, GLenum type, const GLvoid *pixels );

extern void glGetTexImage( GLenum target, GLint level, GLenum format,
      GLenum type, GLvoid *pixels );





extern void glGenTextures( GLsizei n, GLuint *textures );

extern void glDeleteTextures( GLsizei n, const GLuint *textures);

extern void glBindTexture( GLenum target, GLuint texture );

extern void glPrioritizeTextures( GLsizei n, const GLuint *textures,
                                  const GLclampf *priorities );

extern GLboolean glAreTexturesResident( GLsizei n,
                                       const GLuint *textures,
                                        GLboolean *residences );

extern GLboolean glIsTexture( GLuint texture );


extern void glTexSubImage1D( GLenum target, GLint level, GLint xoffset,
                             GLsizei width, GLenum format,
                             GLenum type, const GLvoid *pixels );


extern void glTexSubImage2D( GLenum target, GLint level,
                             GLint xoffset, GLint yoffset,
                             GLsizei width, GLsizei height,
                             GLenum format, GLenum type,
                             const GLvoid *pixels );


extern void glCopyTexImage1D( GLenum target, GLint level,
                              GLenum internalformat,
                              GLint x, GLint y,
                              GLsizei width, GLint border );


extern void glCopyTexImage2D( GLenum target, GLint level,
                              GLenum internalformat,
                              GLint x, GLint y,
                              GLsizei width, GLsizei height, GLint border );


extern void glCopyTexSubImage1D( GLenum target, GLint level,
                                 GLint xoffset, GLint x, GLint y,
                                 GLsizei width );


extern void glCopyTexSubImage2D( GLenum target, GLint level,
                                 GLint xoffset, GLint yoffset,
                                 GLint x, GLint y,
                                 GLsizei width, GLsizei height );
# 1431 "../include/GL/gl.h"
extern void glMap1d( GLenum target, GLdouble u1, GLdouble u2, GLint stride,
       GLint order, const GLdouble *points );
extern void glMap1f( GLenum target, GLfloat u1, GLfloat u2, GLint stride,
       GLint order, const GLfloat *points );

extern void glMap2d( GLenum target,
       GLdouble u1, GLdouble u2, GLint ustride, GLint uorder,
       GLdouble v1, GLdouble v2, GLint vstride, GLint vorder,
       const GLdouble *points );
extern void glMap2f( GLenum target,
       GLfloat u1, GLfloat u2, GLint ustride, GLint uorder,
       GLfloat v1, GLfloat v2, GLint vstride, GLint vorder,
       const GLfloat *points );

extern void glGetMapdv( GLenum target, GLenum query, GLdouble *v );
extern void glGetMapfv( GLenum target, GLenum query, GLfloat *v );
extern void glGetMapiv( GLenum target, GLenum query, GLint *v );

extern void glEvalCoord1d( GLdouble u );
extern void glEvalCoord1f( GLfloat u );

extern void glEvalCoord1dv( const GLdouble *u );
extern void glEvalCoord1fv( const GLfloat *u );

extern void glEvalCoord2d( GLdouble u, GLdouble v );
extern void glEvalCoord2f( GLfloat u, GLfloat v );

extern void glEvalCoord2dv( const GLdouble *u );
extern void glEvalCoord2fv( const GLfloat *u );

extern void glMapGrid1d( GLint un, GLdouble u1, GLdouble u2 );
extern void glMapGrid1f( GLint un, GLfloat u1, GLfloat u2 );

extern void glMapGrid2d( GLint un, GLdouble u1, GLdouble u2,
    GLint vn, GLdouble v1, GLdouble v2 );
extern void glMapGrid2f( GLint un, GLfloat u1, GLfloat u2,
    GLint vn, GLfloat v1, GLfloat v2 );

extern void glEvalPoint1( GLint i );

extern void glEvalPoint2( GLint i, GLint j );

extern void glEvalMesh1( GLenum mode, GLint i1, GLint i2 );

extern void glEvalMesh2( GLenum mode, GLint i1, GLint i2, GLint j1, GLint j2 );







extern void glFogf( GLenum pname, GLfloat param );

extern void glFogi( GLenum pname, GLint param );

extern void glFogfv( GLenum pname, const GLfloat *params );

extern void glFogiv( GLenum pname, const GLint *params );







extern void glFeedbackBuffer( GLsizei size, GLenum type, GLfloat *buffer );

extern void glPassThrough( GLfloat token );

extern void glSelectBuffer( GLsizei size, GLuint *buffer );

extern void glInitNames( void );

extern void glLoadName( GLuint name );

extern void glPushName( GLuint name );

extern void glPopName( void );
# 1518 "../include/GL/gl.h"
extern void glBlendEquationEXT( GLenum mode );




extern void glBlendColorEXT( GLclampf red, GLclampf green,
        GLclampf blue, GLclampf alpha );




extern void glPolygonOffsetEXT( GLfloat factor, GLfloat bias );





extern void glVertexPointerEXT( GLint size, GLenum type, GLsizei stride,
                              GLsizei count, const GLvoid *ptr );

extern void glNormalPointerEXT( GLenum type, GLsizei stride,
    GLsizei count, const GLvoid *ptr );

extern void glColorPointerEXT( GLint size, GLenum type, GLsizei stride,
                               GLsizei count, const GLvoid *ptr );

extern void glIndexPointerEXT( GLenum type, GLsizei stride,
          GLsizei count, const GLvoid *ptr );

extern void glTexCoordPointerEXT( GLint size, GLenum type, GLsizei stride,
                                  GLsizei count, const GLvoid *ptr );

extern void glEdgeFlagPointerEXT( GLsizei stride,
      GLsizei count, const GLboolean *ptr );

extern void glGetPointervEXT( GLenum pname, void **params );

extern void glArrayElementEXT( GLint i );

extern void glDrawArraysEXT( GLenum mode, GLint first, GLsizei count );





extern void glGenTexturesEXT( GLsizei n, GLuint *textures );

extern void glDeleteTexturesEXT( GLsizei n, const GLuint *textures);

extern void glBindTextureEXT( GLenum target, GLuint texture );

extern void glPrioritizeTexturesEXT( GLsizei n, const GLuint *textures,
                                     const GLclampf *priorities );

extern GLboolean glAreTexturesResidentEXT( GLsizei n,
                                           const GLuint *textures,
                                           GLboolean *residences );

extern GLboolean glIsTextureEXT( GLuint texture );





extern void glTexImage3DEXT( GLenum target, GLint level, GLenum internalformat,
                             GLsizei width, GLsizei height, GLsizei depth,
                             GLint border, GLenum format, GLenum type,
                             const GLvoid *pixels );

extern void glTexSubImage3DEXT( GLenum target, GLint level, GLint xoffset,
                                GLint yoffset, GLint zoffset, GLsizei width,
                                GLsizei height, GLsizei depth, GLenum format,
                                GLenum type, const GLvoid *pixels );

extern void glCopyTexSubImage3DEXT( GLenum target, GLint level, GLint xoffset,
                                    GLint yoffset, GLint zoffset,
                                    GLint x, GLint y, GLsizei width,
                                    GLsizei height );




extern void glWindowPos2iMESA( GLint x, GLint y );
extern void glWindowPos2sMESA( GLshort x, GLshort y );
extern void glWindowPos2fMESA( GLfloat x, GLfloat y );
extern void glWindowPos2dMESA( GLdouble x, GLdouble y );

extern void glWindowPos2ivMESA( const GLint *p );
extern void glWindowPos2svMESA( const GLshort *p );
extern void glWindowPos2fvMESA( const GLfloat *p );
extern void glWindowPos2dvMESA( const GLdouble *p );

extern void glWindowPos3iMESA( GLint x, GLint y, GLint z );
extern void glWindowPos3sMESA( GLshort x, GLshort y, GLshort z );
extern void glWindowPos3fMESA( GLfloat x, GLfloat y, GLfloat z );
extern void glWindowPos3dMESA( GLdouble x, GLdouble y, GLdouble z );

extern void glWindowPos3ivMESA( const GLint *p );
extern void glWindowPos3svMESA( const GLshort *p );
extern void glWindowPos3fvMESA( const GLfloat *p );
extern void glWindowPos3dvMESA( const GLdouble *p );

extern void glWindowPos4iMESA( GLint x, GLint y, GLint z, GLint w );
extern void glWindowPos4sMESA( GLshort x, GLshort y, GLshort z, GLshort w );
extern void glWindowPos4fMESA( GLfloat x, GLfloat y, GLfloat z, GLfloat w );
extern void glWindowPos4dMESA( GLdouble x, GLdouble y, GLdouble z, GLdouble w);

extern void glWindowPos4ivMESA( const GLint *p );
extern void glWindowPos4svMESA( const GLshort *p );
extern void glWindowPos4fvMESA( const GLfloat *p );
extern void glWindowPos4dvMESA( const GLdouble *p );




extern void glResizeBuffersMESA();
# 91 "types.h" 2
# 1 "config.h" 1
# 92 "types.h" 2
# 101 "types.h"
   typedef GLshort GLaccum;
# 111 "types.h"
   typedef GLubyte GLstencil;
# 122 "types.h"
   typedef GLushort GLdepth;
# 131 "types.h"
# 1 "fixed.h" 1
# 39 "fixed.h"
typedef int GLfixed;
# 132 "types.h" 2



typedef struct gl_visual GLvisual;

typedef struct gl_context GLcontext;

typedef struct gl_frame_buffer GLframebuffer;






typedef void (*points_func)( GLcontext *ctx, GLuint first, GLuint last );

typedef void (*line_func)( GLcontext *ctx, GLuint v1, GLuint v2, GLuint pv );

typedef void (*polygon_func)( GLcontext *ctx,
                              GLuint n, GLuint vlist[], GLuint pv );

typedef void (*triangle_func)( GLcontext *ctx,
                               GLuint v1, GLuint v2, GLuint v3, GLuint pv );





struct gl_image {
 GLint Width;
 GLint Height;
 GLint Depth;
 GLint Components;
        GLenum Format;
 GLenum Type;
 GLvoid *Data;
 GLboolean Interleaved;





 GLint RefCount;
};




struct gl_texture_image {
 GLenum Format;


 GLuint Border;
 GLuint Width;
 GLuint Height;
 GLuint Depth;
 GLuint Width2;
 GLuint Height2;
 GLuint Depth2;
 GLuint WidthLog2;
 GLuint HeightLog2;
 GLuint DepthLog2;
 GLuint MaxLog2;
 GLubyte *Data;
};






struct api_function_table {
   void (*Accum)( GLcontext *, GLenum, GLfloat );
   void (*AlphaFunc)( GLcontext *, GLenum, GLclampf );
   GLboolean (*AreTexturesResident)( GLcontext *, GLsizei,
                                     const GLuint *, GLboolean * );
   void (*ArrayElement)( GLcontext *, GLint );
   void (*Begin)( GLcontext *, GLenum );
   void (*BindTexture)( GLcontext *, GLenum, GLuint );
   void (*Bitmap)( GLcontext *, GLsizei, GLsizei, GLfloat, GLfloat,
       GLfloat, GLfloat, const struct gl_image *bitmap );
   void (*BlendColor)( GLcontext *, GLclampf, GLclampf, GLclampf, GLclampf);
   void (*BlendEquation)( GLcontext *, GLenum );
   void (*BlendFunc)( GLcontext *, GLenum, GLenum );
   void (*CallList)( GLcontext *, GLuint list );
   void (*CallLists)( GLcontext *, GLsizei, GLenum, const GLvoid * );
   void (*Clear)( GLcontext *, GLbitfield );
   void (*ClearAccum)( GLcontext *, GLfloat, GLfloat, GLfloat, GLfloat );
   void (*ClearColor)( GLcontext *, GLclampf, GLclampf, GLclampf, GLclampf );
   void (*ClearDepth)( GLcontext *, GLclampd );
   void (*ClearIndex)( GLcontext *, GLfloat );
   void (*ClearStencil)( GLcontext *, GLint );
   void (*ClipPlane)( GLcontext *, GLenum, const GLfloat * );
   void (*Color4f)( GLcontext *, GLfloat, GLfloat, GLfloat, GLfloat );
   void (*Color4ub)( GLcontext *, GLubyte, GLubyte, GLubyte, GLubyte );
   void (*ColorMask)( GLcontext *,
   GLboolean, GLboolean, GLboolean, GLboolean );
   void (*ColorMaterial)( GLcontext *, GLenum, GLenum );
   void (*ColorPointer)( GLcontext *, GLint, GLenum, GLsizei, const GLvoid * );
   void (*CopyPixels)( GLcontext *, GLint, GLint, GLsizei, GLsizei, GLenum );
   void (*CopyTexImage1D)( GLcontext *, GLenum, GLint, GLenum,
                           GLint, GLint, GLsizei, GLint );
   void (*CopyTexImage2D)( GLcontext *, GLenum, GLint, GLenum,
                           GLint, GLint, GLsizei, GLsizei, GLint );
   void (*CopyTexSubImage1D)( GLcontext *, GLenum, GLint, GLint,
                              GLint, GLint, GLsizei );
   void (*CopyTexSubImage2D)( GLcontext *, GLenum, GLint, GLint, GLint,
                              GLint, GLint, GLsizei, GLsizei );
   void (*CopyTexSubImage3DEXT)(GLcontext *,
                                GLenum , GLint ,
                                GLint , GLint , GLint ,
                                GLint , GLint ,
                                GLsizei , GLsizei );
   void (*CullFace)( GLcontext *, GLenum );
   void (*DeleteLists)( GLcontext *, GLuint, GLsizei );
   void (*DeleteTextures)( GLcontext *, GLsizei, const GLuint *);
   void (*DepthFunc)( GLcontext *, GLenum );
   void (*DepthMask)( GLcontext *, GLboolean );
   void (*DepthRange)( GLcontext *, GLclampd, GLclampd );
   void (*Disable)( GLcontext *, GLenum );
   void (*DisableClientState)( GLcontext *, GLenum );
   void (*DrawArrays)( GLcontext *, GLenum, GLint, GLsizei );
   void (*DrawBuffer)( GLcontext *, GLenum );
   void (*DrawElements)( GLcontext *, GLenum, GLsizei, GLenum, const GLvoid *);
   void (*DrawPixels)( GLcontext *,
                       GLsizei, GLsizei, GLenum, GLenum, const GLvoid * );
   void (*EdgeFlag)( GLcontext *, GLboolean );
   void (*EdgeFlagPointer)( GLcontext *, GLsizei, const GLboolean * );
   void (*Enable)( GLcontext *, GLenum );
   void (*EnableClientState)( GLcontext *, GLenum );
   void (*End)( GLcontext * );
   void (*EndList)( GLcontext * );
   void (*EvalCoord1f)( GLcontext *, GLfloat );
   void (*EvalCoord2f)( GLcontext *, GLfloat , GLfloat );
   void (*EvalMesh1)( GLcontext *, GLenum, GLint, GLint );
   void (*EvalMesh2)( GLcontext *, GLenum, GLint, GLint, GLint, GLint );
   void (*EvalPoint1)( GLcontext *, GLint );
   void (*EvalPoint2)( GLcontext *, GLint, GLint );
   void (*FeedbackBuffer)( GLcontext *, GLsizei, GLenum, GLfloat * );
   void (*Finish)( GLcontext * );
   void (*Flush)( GLcontext * );
   void (*Fogfv)( GLcontext *, GLenum, const GLfloat * );
   void (*FrontFace)( GLcontext *, GLenum );
   void (*Frustum)( GLcontext *, GLdouble, GLdouble, GLdouble, GLdouble,
        GLdouble, GLdouble );
   GLuint (*GenLists)( GLcontext *, GLsizei );
   void (*GenTextures)( GLcontext *, GLsizei, GLuint * );
   void (*GetBooleanv)( GLcontext *, GLenum, GLboolean * );
   void (*GetClipPlane)( GLcontext *, GLenum, GLdouble * );
   void (*GetDoublev)( GLcontext *, GLenum, GLdouble * );
   GLenum (*GetError)( GLcontext * );
   void (*GetFloatv)( GLcontext *, GLenum, GLfloat * );
   void (*GetIntegerv)( GLcontext *, GLenum, GLint * );
   const GLubyte* (*GetString)( GLcontext *, GLenum name );
   void (*GetLightfv)( GLcontext *, GLenum light, GLenum, GLfloat * );
   void (*GetLightiv)( GLcontext *, GLenum light, GLenum, GLint * );
   void (*GetMapdv)( GLcontext *, GLenum, GLenum, GLdouble * );
   void (*GetMapfv)( GLcontext *, GLenum, GLenum, GLfloat * );
   void (*GetMapiv)( GLcontext *, GLenum, GLenum, GLint * );
   void (*GetMaterialfv)( GLcontext *, GLenum, GLenum, GLfloat * );
   void (*GetMaterialiv)( GLcontext *, GLenum, GLenum, GLint * );
   void (*GetPixelMapfv)( GLcontext *, GLenum, GLfloat * );
   void (*GetPixelMapuiv)( GLcontext *, GLenum, GLuint * );
   void (*GetPixelMapusv)( GLcontext *, GLenum, GLushort * );
   void (*GetPointerv)( GLcontext *, GLenum, GLvoid ** );
   void (*GetPolygonStipple)( GLcontext *, GLubyte * );
   void (*PrioritizeTextures)( GLcontext *, GLsizei, const GLuint *,
                               const GLclampf * );
   void (*GetTexEnvfv)( GLcontext *, GLenum, GLenum, GLfloat * );
   void (*GetTexEnviv)( GLcontext *, GLenum, GLenum, GLint * );
   void (*GetTexGendv)( GLcontext *, GLenum coord, GLenum, GLdouble * );
   void (*GetTexGenfv)( GLcontext *, GLenum coord, GLenum, GLfloat * );
   void (*GetTexGeniv)( GLcontext *, GLenum coord, GLenum, GLint * );
   void (*GetTexImage)( GLcontext *, GLenum, GLint level, GLenum, GLenum,
                        GLvoid * );
   void (*GetTexLevelParameterfv)( GLcontext *,
         GLenum, GLint, GLenum, GLfloat * );
   void (*GetTexLevelParameteriv)( GLcontext *,
         GLenum, GLint, GLenum, GLint * );
   void (*GetTexParameterfv)( GLcontext *, GLenum, GLenum, GLfloat *);
   void (*GetTexParameteriv)( GLcontext *, GLenum, GLenum, GLint * );
   void (*Hint)( GLcontext *, GLenum, GLenum );
   void (*IndexMask)( GLcontext *, GLuint );
   void (*Indexf)( GLcontext *, GLfloat c );
   void (*Indexi)( GLcontext *, GLint c );
   void (*IndexPointer)( GLcontext *, GLenum, GLsizei, const GLvoid * );
   void (*InitNames)( GLcontext * );
   void (*InterleavedArrays)( GLcontext *, GLenum, GLsizei, const GLvoid * );
   GLboolean (*IsEnabled)( GLcontext *, GLenum );
   GLboolean (*IsList)( GLcontext *, GLuint );
   GLboolean (*IsTexture)( GLcontext *, GLuint );
   void (*LightModelfv)( GLcontext *, GLenum, const GLfloat * );
   void (*Lightfv)( GLcontext *, GLenum light, GLenum, const GLfloat *, GLint);
   void (*LineStipple)( GLcontext *, GLint factor, GLushort );
   void (*LineWidth)( GLcontext *, GLfloat );
   void (*ListBase)( GLcontext *, GLuint );


   void (*LoadMatrixf)( GLcontext *, const GLfloat * );
   void (*LoadName)( GLcontext *, GLuint );
   void (*LogicOp)( GLcontext *, GLenum );
   void (*Map1f)( GLcontext *, GLenum, GLfloat, GLfloat, GLint, GLint,
    const GLfloat *, GLboolean );
   void (*Map2f)( GLcontext *, GLenum, GLfloat, GLfloat, GLint, GLint,
    GLfloat, GLfloat, GLint, GLint, const GLfloat *,
    GLboolean );
   void (*MapGrid1f)( GLcontext *, GLint, GLfloat, GLfloat );
   void (*MapGrid2f)( GLcontext *, GLint, GLfloat, GLfloat,
   GLint, GLfloat, GLfloat );
   void (*Materialfv)( GLcontext *, GLenum, GLenum, const GLfloat * );
   void (*MatrixMode)( GLcontext *, GLenum );

   void (*MultMatrixf)( GLcontext *, const GLfloat * );
   void (*NewList)( GLcontext *, GLuint list, GLenum );
   void (*Normal3f)( GLcontext *, GLfloat, GLfloat, GLfloat );
   void (*Normal3fv)( GLcontext *, const GLfloat * );
   void (*NormalPointer)( GLcontext *, GLenum, GLsizei, const GLvoid * );

   void (*PassThrough)( GLcontext *, GLfloat );
   void (*PixelMapfv)( GLcontext *, GLenum, GLint, const GLfloat * );
   void (*PixelStorei)( GLcontext *, GLenum, GLint );
   void (*PixelTransferf)( GLcontext *, GLenum, GLfloat );
   void (*PixelZoom)( GLcontext *, GLfloat, GLfloat );
   void (*PointSize)( GLcontext *, GLfloat );
   void (*PolygonMode)( GLcontext *, GLenum, GLenum );
   void (*PolygonOffset)( GLcontext *, GLfloat, GLfloat );
   void (*PolygonStipple)( GLcontext *, const GLubyte * );
   void (*PopAttrib)( GLcontext * );
   void (*PopClientAttrib)( GLcontext * );
   void (*PopMatrix)( GLcontext * );
   void (*PopName)( GLcontext * );
   void (*PushAttrib)( GLcontext *, GLbitfield );
   void (*PushClientAttrib)( GLcontext *, GLbitfield );
   void (*PushMatrix)( GLcontext * );
   void (*PushName)( GLcontext *, GLuint );
   void (*RasterPos4f)( GLcontext *,
                        GLfloat x, GLfloat y, GLfloat z, GLfloat w );
   void (*ReadBuffer)( GLcontext *, GLenum );
   void (*ReadPixels)( GLcontext *, GLint, GLint, GLsizei, GLsizei, GLenum,
    GLenum, GLvoid * );
   void (*Rectf)( GLcontext *, GLfloat, GLfloat, GLfloat, GLfloat );
   GLint (*RenderMode)( GLcontext *, GLenum );
   void (*Rotatef)( GLcontext *, GLfloat, GLfloat, GLfloat, GLfloat );
   void (*Scalef)( GLcontext *, GLfloat, GLfloat, GLfloat );
   void (*Scissor)( GLcontext *, GLint, GLint, GLsizei, GLsizei);
   void (*SelectBuffer)( GLcontext *, GLsizei, GLuint * );
   void (*ShadeModel)( GLcontext *, GLenum );
   void (*StencilFunc)( GLcontext *, GLenum, GLint, GLuint );
   void (*StencilMask)( GLcontext *, GLuint );
   void (*StencilOp)( GLcontext *, GLenum, GLenum, GLenum );
   void (*TexCoord4f)( GLcontext *, GLfloat, GLfloat, GLfloat, GLfloat );
   void (*TexCoordPointer)( GLcontext *, GLint, GLenum, GLsizei,
                            const GLvoid *);
   void (*TexEnvfv)( GLcontext *, GLenum, GLenum, const GLfloat * );
   void (*TexGenfv)( GLcontext *, GLenum coord, GLenum, const GLfloat * );
   void (*TexImage1D)( GLcontext *, GLenum, GLint, GLint, GLsizei,
                       GLint, GLenum, GLenum, struct gl_image * );
   void (*TexImage2D)( GLcontext *, GLenum, GLint, GLint, GLsizei, GLsizei,
                       GLint, GLenum, GLenum, struct gl_image * );
   void (*TexSubImage1D)( GLcontext *, GLenum, GLint, GLint, GLsizei,
                          GLenum, GLenum, struct gl_image * );
   void (*TexSubImage2D)( GLcontext *, GLenum, GLint, GLint, GLint,
                          GLsizei, GLsizei, GLenum, GLenum,
                          struct gl_image * );
   void (*TexImage3DEXT)(GLcontext *,
                         GLenum , GLint , GLint ,
                         GLsizei , GLsizei , GLsizei ,
                         GLint ,
                         GLenum , GLenum ,
                         struct gl_image * );
   void (*TexSubImage3DEXT)(GLcontext *,
                            GLenum , GLint ,
                            GLint , GLint , GLint,
                            GLsizei , GLsizei , GLsizei ,
                            GLenum , GLenum ,
                            struct gl_image * );
   void (*TexParameterfv)( GLcontext *, GLenum, GLenum, const GLfloat * );

   void (*Translatef)( GLcontext *, GLfloat, GLfloat, GLfloat );
   void (*Vertex4f)( GLcontext *, GLfloat, GLfloat, GLfloat, GLfloat );
   void (*VertexPointer)( GLcontext *, GLint, GLenum, GLsizei, const GLvoid *);
   void (*Viewport)( GLcontext *, GLint, GLint, GLsizei, GLsizei );


   void (*WindowPos4fMESA)( GLcontext *, GLfloat, GLfloat, GLfloat, GLfloat );


   void (*ResizeBuffersMESA)( GLcontext * );
};



# 1 "dd.h" 1
# 120 "dd.h"
struct dd_function_table {






   void (*UpdateState)( GLcontext *ctx );






   void (*ClearIndex)( GLcontext *ctx, GLuint index );





   void (*ClearColor)( GLcontext *ctx, GLubyte red, GLubyte green,
                                        GLubyte blue, GLubyte alpha );





   void (*Clear)( GLcontext *ctx,
                  GLboolean all, GLint x, GLint y, GLint width, GLint height );





   void (*Index)( GLcontext *ctx, GLuint index );




   void (*Color)( GLcontext *ctx,
                  GLubyte red, GLubyte green, GLubyte glue, GLubyte alpha );




   GLboolean (*SetBuffer)( GLcontext *ctx, GLenum mode );





   void (*GetBufferSize)( GLcontext *ctx,
                          GLuint *width, GLuint *height );
# 181 "dd.h"
   void (*WriteColorSpan)( GLcontext *ctx,
                           GLuint n, GLint x, GLint y,
      const GLubyte red[], const GLubyte green[],
      const GLubyte blue[], const GLubyte alpha[],
      const GLubyte mask[] );




   void (*WriteMonocolorSpan)( GLcontext *ctx,
                               GLuint n, GLint x, GLint y,
          const GLubyte mask[] );




   void (*WriteColorPixels)( GLcontext *ctx,
                             GLuint n, const GLint x[], const GLint y[],
        const GLubyte red[], const GLubyte green[],
        const GLubyte blue[], const GLubyte alpha[],
        const GLubyte mask[] );




   void (*WriteMonocolorPixels)( GLcontext *ctx,
                                 GLuint n, const GLint x[], const GLint y[],
     const GLubyte mask[] );




   void (*WriteIndexSpan)( GLcontext *ctx,
                           GLuint n, GLint x, GLint y, const GLuint index[],
                           const GLubyte mask[] );




   void (*WriteMonoindexSpan)( GLcontext *ctx,
                               GLuint n, GLint x, GLint y,
          const GLubyte mask[] );




   void (*WriteIndexPixels)( GLcontext *ctx,
                             GLuint n, const GLint x[], const GLint y[],
                             const GLuint index[], const GLubyte mask[] );




   void (*WriteMonoindexPixels)( GLcontext *ctx,
                                 GLuint n, const GLint x[], const GLint y[],
     const GLubyte mask[] );







   void (*ReadIndexSpan)( GLcontext *ctx,
                          GLuint n, GLint x, GLint y, GLuint index[] );




   void (*ReadColorSpan)( GLcontext *ctx,
                          GLuint n, GLint x, GLint y,
     GLubyte red[], GLubyte green[],
     GLubyte blue[], GLubyte alpha[] );




   void (*ReadIndexPixels)( GLcontext *ctx,
                            GLuint n, const GLint x[], const GLint y[],
       GLuint indx[], const GLubyte mask[] );




   void (*ReadColorPixels)( GLcontext *ctx,
                            GLuint n, const GLint x[], const GLint y[],
       GLubyte red[], GLubyte green[],
       GLubyte blue[], GLubyte alpha[],
                            const GLubyte mask[] );
# 283 "dd.h"
   void (*Finish)( GLcontext *ctx );




   void (*Flush)( GLcontext *ctx );




   GLboolean (*IndexMask)( GLcontext *ctx, GLuint mask );




   GLboolean (*ColorMask)( GLcontext *ctx,
                           GLboolean rmask, GLboolean gmask,
                           GLboolean bmask, GLboolean amask );




   GLboolean (*LogicOp)( GLcontext *ctx, GLenum op );




   void (*Dither)( GLcontext *ctx, GLboolean enable );




   void (*Error)( GLcontext *ctx );
# 326 "dd.h"
   void (*AllocDepthBuffer)( GLcontext *ctx );




   void (*ClearDepthBuffer)( GLcontext *ctx );




   GLuint (*DepthTestSpan)( GLcontext *ctx,
                            GLuint n, GLint x, GLint y, const GLdepth z[],
                            GLubyte mask[] );
   void (*DepthTestPixels)( GLcontext *ctx,
                            GLuint n, const GLint x[], const GLint y[],
                            const GLdepth z[], GLubyte mask[] );






   void (*ReadDepthSpanFloat)( GLcontext *ctx,
                               GLuint n, GLint x, GLint y, GLfloat depth[]);
   void (*ReadDepthSpanInt)( GLcontext *ctx,
                             GLuint n, GLint x, GLint y, GLdepth depth[] );
# 363 "dd.h"
   points_func PointsFunc;




   line_func LineFunc;




   triangle_func TriangleFunc;
# 383 "dd.h"
   GLboolean (*DrawPixels)( GLcontext *ctx,
                            GLint x, GLint y, GLsizei width, GLsizei height,
                            GLenum format, GLenum type, GLboolean packed,
                            const GLvoid *pixels );




   GLboolean (*Bitmap)( GLcontext *ctx, GLsizei width, GLsizei height,
                        GLfloat xorig, GLfloat yorig,
                        GLfloat xmove, GLfloat ymove,
                        const struct gl_image *bitmap );




   void (*Begin)( GLcontext *ctx, GLenum mode );
   void (*End)( GLcontext *ctx );
# 411 "dd.h"
   void (*TexEnv)( GLcontext *ctx, GLenum pname, const GLfloat *param );







   void (*TexImage)( GLcontext *ctx, GLenum target,
                     GLuint texObject, GLint level, GLint internalFormat,
                     const struct gl_texture_image *image );
# 431 "dd.h"
   void (*TexParameter)( GLcontext *ctx, GLenum target, GLuint texObject,
                         GLenum pname, const GLfloat *params );
# 442 "dd.h"
   void (*BindTexture)( GLcontext *ctx, GLenum target, GLuint texObject );





   void (*DeleteTexture)( GLcontext *ctx, GLuint texObject );




};
# 425 "types.h" 2
# 461 "types.h"
struct gl_light {
 GLfloat Ambient[4];
 GLfloat Diffuse[4];
 GLfloat Specular[4];
 GLfloat Position[4];
 GLfloat Direction[4];
 GLfloat SpotExponent;
 GLfloat SpotCutoff;
        GLfloat CosCutoff;
 GLfloat ConstantAttenuation;
 GLfloat LinearAttenuation;
 GLfloat QuadraticAttenuation;
 GLboolean Enabled;

 struct gl_light *NextEnabled;


 GLfloat VP_inf_norm[3];
 GLfloat h_inf_norm[3];
        GLfloat NormDirection[3];
        GLfloat SpotExpTable[512][2];
 GLfloat MatAmbient[3];
 GLfloat MatDiffuse[3];
 GLfloat MatSpecular[3];
 GLfloat dli;
 GLfloat sli;
};


struct gl_lightmodel {
 GLfloat Ambient[4];
 GLboolean LocalViewer;
 GLboolean TwoSide;
};


struct gl_material {
 GLfloat Ambient[4];
 GLfloat Diffuse[4];
 GLfloat Specular[4];
 GLfloat Emission[4];
 GLfloat Shininess;
 GLfloat AmbientIndex;
 GLfloat DiffuseIndex;
 GLfloat SpecularIndex;
        GLfloat ShineTable[200];
};
# 518 "types.h"
struct gl_accum_attrib {
 GLfloat ClearColor[4];
};


struct gl_colorbuffer_attrib {
 GLuint ClearIndex;
 GLfloat ClearColor[4];

 GLuint IndexMask;
 GLuint ColorMask;
        GLboolean SWmasking;

 GLenum DrawBuffer;


 GLboolean AlphaEnabled;
 GLenum AlphaFunc;
 GLfloat AlphaRef;
 GLubyte AlphaRefUbyte;


 GLboolean BlendEnabled;
 GLenum BlendSrc;
 GLenum BlendDst;
 GLenum BlendEquation;
 GLfloat BlendColor[4];


 GLenum LogicOp;
 GLboolean IndexLogicOpEnabled;
 GLboolean ColorLogicOpEnabled;
 GLboolean SWLogicOpEnabled;

 GLboolean DitherFlag;
};


struct gl_current_attrib {
 GLint IntColor[4];
 GLuint Index;
 GLfloat Normal[3];
 GLfloat TexCoord[4];
 GLfloat RasterPos[4];
 GLfloat RasterDistance;
 GLfloat RasterColor[4];
 GLuint RasterIndex;
 GLfloat RasterTexCoord[4];
 GLboolean RasterPosValid;
 GLboolean EdgeFlag;
};


struct gl_depthbuffer_attrib {
 GLenum Func;
 GLfloat Clear;
 GLboolean Test;
 GLboolean Mask;
};


struct gl_enable_attrib {
 GLboolean AlphaTest;
 GLboolean AutoNormal;
 GLboolean Blend;
 GLboolean ClipPlane[6];
 GLboolean ColorMaterial;
 GLboolean CullFace;
 GLboolean DepthTest;
 GLboolean Dither;
 GLboolean Fog;
 GLboolean Light[8];
 GLboolean Lighting;
 GLboolean LineSmooth;
 GLboolean LineStipple;
 GLboolean IndexLogicOp;
 GLboolean ColorLogicOp;
 GLboolean Map1Color4;
 GLboolean Map1Index;
 GLboolean Map1Normal;
 GLboolean Map1TextureCoord1;
 GLboolean Map1TextureCoord2;
 GLboolean Map1TextureCoord3;
 GLboolean Map1TextureCoord4;
 GLboolean Map1Vertex3;
 GLboolean Map1Vertex4;
 GLboolean Map2Color4;
 GLboolean Map2Index;
 GLboolean Map2Normal;
 GLboolean Map2TextureCoord1;
 GLboolean Map2TextureCoord2;
 GLboolean Map2TextureCoord3;
 GLboolean Map2TextureCoord4;
 GLboolean Map2Vertex3;
 GLboolean Map2Vertex4;
 GLboolean Normalize;
 GLboolean PointSmooth;
 GLboolean PolygonOffsetPoint;
 GLboolean PolygonOffsetLine;
 GLboolean PolygonOffsetFill;
 GLboolean PolygonSmooth;
 GLboolean PolygonStipple;
 GLboolean Scissor;
 GLboolean Stencil;
 GLuint Texture;
 GLuint TexGen;
};


struct gl_eval_attrib {

 GLboolean Map1Color4;
 GLboolean Map1Index;
 GLboolean Map1Normal;
 GLboolean Map1TextureCoord1;
 GLboolean Map1TextureCoord2;
 GLboolean Map1TextureCoord3;
 GLboolean Map1TextureCoord4;
 GLboolean Map1Vertex3;
 GLboolean Map1Vertex4;
 GLboolean Map2Color4;
 GLboolean Map2Index;
 GLboolean Map2Normal;
 GLboolean Map2TextureCoord1;
 GLboolean Map2TextureCoord2;
 GLboolean Map2TextureCoord3;
 GLboolean Map2TextureCoord4;
 GLboolean Map2Vertex3;
 GLboolean Map2Vertex4;
 GLboolean AutoNormal;

 GLuint MapGrid1un;
 GLfloat MapGrid1u1, MapGrid1u2;
 GLuint MapGrid2un, MapGrid2vn;
 GLfloat MapGrid2u1, MapGrid2u2;
 GLfloat MapGrid2v1, MapGrid2v2;
};


struct gl_fog_attrib {
 GLboolean Enabled;
 GLfloat Color[4];
 GLfloat Density;
 GLfloat Start;
 GLfloat End;
 GLfloat Index;
 GLenum Mode;
};


struct gl_hint_attrib {

 GLenum PerspectiveCorrection;
 GLenum PointSmooth;
 GLenum LineSmooth;
 GLenum PolygonSmooth;
 GLenum Fog;
};


struct gl_light_attrib {
   struct gl_light Light[8];
   struct gl_lightmodel Model;
   struct gl_material Material[2];
   GLboolean Enabled;
   GLenum ShadeModel;
   GLenum ColorMaterialFace;
   GLenum ColorMaterialMode;
   GLuint ColorMaterialBitmask;
   GLboolean ColorMaterialEnabled;


   struct gl_light *FirstEnabled;
   GLboolean Fast;
   GLfloat BaseColor[4];
};


struct gl_line_attrib {
 GLboolean SmoothFlag;
 GLboolean StippleFlag;
 GLushort StipplePattern;
 GLint StippleFactor;
 GLfloat Width;
};


struct gl_list_attrib {
 GLuint ListBase;
};


struct gl_pixel_attrib {
 GLenum ReadBuffer;
 GLfloat RedBias, RedScale;
 GLfloat GreenBias, GreenScale;
 GLfloat BlueBias, BlueScale;
 GLfloat AlphaBias, AlphaScale;
 GLfloat DepthBias, DepthScale;
 GLint IndexShift;
 GLint IndexOffset;
 GLboolean MapColorFlag;
 GLboolean MapStencilFlag;
 GLfloat ZoomX;
 GLfloat ZoomY;

 GLint MapStoSsize;
 GLint MapItoIsize;
 GLint MapItoRsize;
 GLint MapItoGsize;
 GLint MapItoBsize;
 GLint MapItoAsize;
 GLint MapRtoRsize;
 GLint MapGtoGsize;
 GLint MapBtoBsize;
 GLint MapAtoAsize;
 GLint MapStoS[256];
 GLint MapItoI[256];
 GLfloat MapItoR[256];
 GLfloat MapItoG[256];
 GLfloat MapItoB[256];
 GLfloat MapItoA[256];
 GLfloat MapRtoR[256];
 GLfloat MapGtoG[256];
 GLfloat MapBtoB[256];
 GLfloat MapAtoA[256];
};


struct gl_point_attrib {
 GLboolean SmoothFlag;
 GLfloat Size;
};


struct gl_polygon_attrib {
 GLenum FrontFace;
 GLenum FrontMode;
 GLenum BackMode;
 GLboolean Unfilled;
 GLboolean CullFlag;
 GLenum CullFaceMode;
        GLuint CullBits;
 GLboolean SmoothFlag;
 GLboolean StippleFlag;
        GLfloat OffsetFactor;
        GLfloat OffsetUnits;
        GLboolean OffsetPoint;
        GLboolean OffsetLine;
        GLboolean OffsetFill;
        GLboolean OffsetAny;
};


struct gl_scissor_attrib {
 GLboolean Enabled;
 GLint X, Y;
 GLsizei Width, Height;
};


struct gl_stencil_attrib {
 GLboolean Enabled;
 GLenum Function;
 GLenum FailFunc;
 GLenum ZPassFunc;
 GLenum ZFailFunc;
 GLstencil Ref;
 GLstencil ValueMask;
 GLstencil Clear;
 GLstencil WriteMask;
};
# 802 "types.h"
struct gl_texture_attrib {
 GLuint Enabled;
 GLenum EnvMode;
 GLfloat EnvColor[4];
 GLuint TexGenEnabled;
 GLenum GenModeS;
 GLenum GenModeT;
 GLenum GenModeR;
 GLenum GenModeQ;
 GLfloat ObjectPlaneS[4];
 GLfloat ObjectPlaneT[4];
 GLfloat ObjectPlaneR[4];
 GLfloat ObjectPlaneQ[4];
 GLfloat EyePlaneS[4];
 GLfloat EyePlaneT[4];
 GLfloat EyePlaneR[4];
 GLfloat EyePlaneQ[4];
 struct gl_texture_object *Current1D;
 struct gl_texture_object *Current2D;
 struct gl_texture_object *Current3D;

 struct gl_texture_object *Proxy1D;
 struct gl_texture_object *Proxy2D;
 struct gl_texture_object *Proxy3D;

};


struct gl_transform_attrib {
 GLenum MatrixMode;
 GLfloat ClipEquation[6][4];
 GLboolean ClipEnabled[6];
 GLboolean AnyClip;
 GLboolean Normalize;
};


struct gl_viewport_attrib {
 GLint X, Y;
 GLsizei Width, Height;
 GLfloat Near, Far;
 GLfloat Sx, Sy, Sz;
 GLfloat Tx, Ty, Tz;
};



struct gl_attrib_node {
 GLbitfield kind;
 void *data;
 struct gl_attrib_node *next;
};






struct gl_pixelstore_attrib {
 GLint Alignment;
 GLint RowLength;
 GLint SkipPixels;
 GLint SkipRows;
 GLint ImageHeight;
 GLint SkipImages;
 GLboolean SwapBytes;
 GLboolean LsbFirst;
};





struct gl_array_attrib {
 GLint VertexSize;
 GLenum VertexType;
 GLsizei VertexStride;
 GLsizei VertexStrideB;
 void *VertexPtr;
 GLboolean VertexEnabled;

 GLenum NormalType;
 GLsizei NormalStride;
 GLsizei NormalStrideB;
 void *NormalPtr;
 GLboolean NormalEnabled;

 GLint ColorSize;
 GLenum ColorType;
 GLsizei ColorStride;
 GLsizei ColorStrideB;
 void *ColorPtr;
 GLboolean ColorEnabled;

 GLenum IndexType;
 GLsizei IndexStride;
 GLsizei IndexStrideB;
 void *IndexPtr;
 GLboolean IndexEnabled;

 GLint TexCoordSize;
 GLenum TexCoordType;
 GLsizei TexCoordStride;
 GLsizei TexCoordStrideB;
 void *TexCoordPtr;
 GLboolean TexCoordEnabled;

 GLsizei EdgeFlagStride;
 GLsizei EdgeFlagStrideB;
 GLboolean *EdgeFlagPtr;
 GLboolean EdgeFlagEnabled;
};



struct gl_feedback {
 GLenum Type;
 GLuint Mask;
 GLfloat *Buffer;
 GLuint BufferSize;
 GLuint Count;
};



struct gl_selection {
 GLuint *Buffer;
 GLuint BufferSize;
 GLuint BufferCount;
 GLuint Hits;
 GLuint NameStackDepth;
 GLuint NameStack[64];
 GLboolean HitFlag;
 GLfloat HitMinZ, HitMaxZ;
};






struct gl_1d_map {
 GLuint Order;
 GLfloat u1, u2;
 GLfloat *Points;
 GLboolean Retain;
};





struct gl_2d_map {
 GLuint Uorder;
 GLuint Vorder;
 GLfloat u1, u2;
 GLfloat v1, v2;
 GLfloat *Points;
 GLboolean Retain;
};





struct gl_evaluators {

 struct gl_1d_map Map1Vertex3;
 struct gl_1d_map Map1Vertex4;
 struct gl_1d_map Map1Index;
 struct gl_1d_map Map1Color4;
 struct gl_1d_map Map1Normal;
 struct gl_1d_map Map1Texture1;
 struct gl_1d_map Map1Texture2;
 struct gl_1d_map Map1Texture3;
 struct gl_1d_map Map1Texture4;


 struct gl_2d_map Map2Vertex3;
 struct gl_2d_map Map2Vertex4;
 struct gl_2d_map Map2Index;
 struct gl_2d_map Map2Color4;
 struct gl_2d_map Map2Normal;
 struct gl_2d_map Map2Texture1;
 struct gl_2d_map Map2Texture2;
 struct gl_2d_map Map2Texture3;
 struct gl_2d_map Map2Texture4;
};




struct gl_texture_object {
 GLint RefCount;
 GLuint Name;
 GLuint Dimensions;
 GLfloat Priority;
 GLint BorderColor[4];
 GLenum WrapS;
 GLenum WrapT;
 GLenum WrapR;
 GLenum MinFilter;
 GLenum MagFilter;
 struct gl_texture_image *Image[11];
 GLboolean Complete;
 struct gl_texture_object *Next;
};





union node;





struct gl_shared_state {
 GLint RefCount;
 union node *List[7000];
 struct gl_texture_object *TexObjectList;
};




struct gl_list_group {
 union node *List[7000];
 GLint RefCount;
};







struct gl_visual {
 GLboolean RGBAflag;
 GLboolean DBflag;

 GLfloat RedScale;
 GLfloat GreenScale;
 GLfloat BlueScale;
 GLfloat AlphaScale;


 GLboolean EightBitColor;


        GLfloat InvRedScale;
        GLfloat InvGreenScale;
        GLfloat InvBlueScale;
        GLfloat InvAlphaScale;

 GLint IndexBits;

 GLint AccumBits;
 GLint DepthBits;
 GLint StencilBits;


 GLboolean FrontAlphaEnabled;
 GLboolean BackAlphaEnabled;
};







struct gl_frame_buffer {
 GLvisual *Visual;

 GLint Width;
 GLint Height;

 GLdepth *Depth;


 GLstencil *Stencil;


 GLaccum *Accum;


 GLubyte *FrontAlpha;
 GLubyte *BackAlpha;
 GLubyte *Alpha;


 GLint Xmin, Xmax, Ymin, Ymax;




};
# 1146 "types.h"
struct gl_context {

 struct gl_shared_state *Shared;


 struct api_function_table API;
 struct api_function_table Save;
 struct api_function_table Exec;

        GLvisual *Visual;
        GLframebuffer *Buffer;


 struct dd_function_table Driver;


 void *DriverCtx;


 GLfloat ModelViewMatrix[16];
 GLfloat ModelViewInv[16];
 GLboolean ModelViewInvValid;
 GLuint ModelViewStackDepth;
 GLfloat ModelViewStack[32][16];


 GLfloat ProjectionMatrix[16];
 GLuint ProjectionStackDepth;
 GLfloat ProjectionStack[32][16];


 GLfloat TextureMatrix[16];
        GLboolean IdentityTexMat;
 GLuint TextureStackDepth;
 GLfloat TextureStack[10][16];


 GLuint CallDepth;
 GLboolean ExecuteFlag;
 GLboolean CompileFlag;


 GLuint AttribStackDepth;
 struct gl_attrib_node *AttribStack[16];


 struct gl_accum_attrib Accum;
 struct gl_colorbuffer_attrib Color;
 struct gl_current_attrib Current;
 struct gl_depthbuffer_attrib Depth;
 struct gl_eval_attrib Eval;
 struct gl_fog_attrib Fog;
 struct gl_hint_attrib Hint;
 struct gl_light_attrib Light;
 struct gl_line_attrib Line;
 struct gl_list_attrib List;
 struct gl_pixel_attrib Pixel;
 struct gl_point_attrib Point;
 struct gl_polygon_attrib Polygon;
 GLuint PolygonStipple[32];
 struct gl_scissor_attrib Scissor;
 struct gl_stencil_attrib Stencil;
 struct gl_texture_attrib Texture;
 struct gl_transform_attrib Transform;
 struct gl_viewport_attrib Viewport;


 GLuint ClientAttribStackDepth;
 struct gl_attrib_node *ClientAttribStack[16];

 struct gl_array_attrib Array;
 struct gl_pixelstore_attrib Pack;
 struct gl_pixelstore_attrib Unpack;

 struct gl_evaluators EvalMap;
 struct gl_feedback Feedback;
 struct gl_selection Select;

 GLenum ErrorValue;


        GLuint NewState;
 GLenum RenderMode;
 GLenum Primitive;
 GLuint StippleCounter;
 GLuint ClipMask;
 GLuint RasterMask;
 GLuint LightTwoSide;
 GLboolean DirectTriangles;
 GLfloat PolygonZoffset;
 GLfloat LineZoffset;
 GLfloat PointZoffset;
 GLboolean NeedNormals;
        GLboolean FastDrawPixels;
        GLboolean MutablePixels;
        GLboolean MonoPixels;
        GLint ColorShift;



        points_func PointsFunc;
        line_func LineFunc;



        polygon_func PolygonFunc;
        polygon_func AuxPolygonFunc;
        triangle_func TriangleFunc;


 struct vertex_buffer* VB;


 struct pixel_buffer* PB;
# 1280 "types.h"
        GLboolean NoRaster;
};
# 49 "context.h" 2
# 61 "context.h"
   extern GLcontext *CC;
# 87 "context.h"
extern GLvisual *gl_create_visual( GLboolean rgb_flag,
                                   GLboolean alpha_flag,
                                   GLboolean db_flag,
                                   GLint depth_bits,
                                   GLint stencil_bits,
                                   GLint accum_bits,
                                   GLint index_bits,
                                   GLfloat red_scale,
                                   GLfloat green_scale,
                                   GLfloat blue_scale,
                                   GLfloat alpha_scale );

extern void gl_destroy_visual( GLvisual *vis );






extern GLcontext *gl_create_context( GLvisual *visual,
                                     GLcontext *share_list,
                                     void *driver_ctx );

extern void gl_destroy_context( GLcontext *ctx );







extern GLframebuffer *gl_create_framebuffer( GLvisual *visual );

extern void gl_destroy_framebuffer( GLframebuffer *buffer );



extern void gl_make_current( GLcontext *ctx, GLframebuffer *buffer );

extern void gl_copy_context( GLcontext *src, GLcontext *dst, GLuint mask );






extern void gl_ResizeBuffersMESA( GLcontext *ctx );







extern void gl_problem( GLcontext *ctx, const char *s );

extern void gl_warning( GLcontext *ctx, const char *s );

extern void gl_error( GLcontext *ctx, GLenum error, const char *s );

extern GLenum gl_GetError( GLcontext *ctx );


extern void gl_update_state( GLcontext *ctx );
# 36 "scissor.c" 2
# 1 "macros.h" 1
# 50 "macros.h"
# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/math.h" 1 3 4
# 28 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/math.h" 3 4
# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/features.h" 1 3 4
# 313 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/features.h" 3 4
# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/predefs.h" 1 3 4
# 314 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/features.h" 2 3 4
# 346 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/features.h" 3 4
# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/sys/cdefs.h" 1 3 4
# 353 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/sys/cdefs.h" 3 4
# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/wordsize.h" 1 3 4
# 354 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/sys/cdefs.h" 2 3 4
# 347 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/features.h" 2 3 4
# 378 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/features.h" 3 4
# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/gnu/stubs.h" 1 3 4
# 379 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/features.h" 2 3 4
# 29 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/math.h" 2 3 4





# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/huge_val.h" 1 3 4
# 35 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/math.h" 2 3 4

# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/huge_valf.h" 1 3 4
# 37 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/math.h" 2 3 4
# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/huge_vall.h" 1 3 4
# 38 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/math.h" 2 3 4


# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/inf.h" 1 3 4
# 41 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/math.h" 2 3 4


# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/nan.h" 1 3 4
# 44 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/math.h" 2 3 4



# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/mathdef.h" 1 3 4
# 28 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/mathdef.h" 3 4
typedef float float_t;

typedef double double_t;
# 48 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/math.h" 2 3 4
# 71 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/math.h" 3 4
# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/mathcalls.h" 1 3 4
# 53 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/mathcalls.h" 3 4


extern double acos (double __x) __attribute__ ((__nothrow__)); extern double __acos (double __x) __attribute__ ((__nothrow__));

extern double asin (double __x) __attribute__ ((__nothrow__)); extern double __asin (double __x) __attribute__ ((__nothrow__));

extern double atan (double __x) __attribute__ ((__nothrow__)); extern double __atan (double __x) __attribute__ ((__nothrow__));

extern double atan2 (double __y, double __x) __attribute__ ((__nothrow__)); extern double __atan2 (double __y, double __x) __attribute__ ((__nothrow__));


extern double cos (double __x) __attribute__ ((__nothrow__)); extern double __cos (double __x) __attribute__ ((__nothrow__));

extern double sin (double __x) __attribute__ ((__nothrow__)); extern double __sin (double __x) __attribute__ ((__nothrow__));

extern double tan (double __x) __attribute__ ((__nothrow__)); extern double __tan (double __x) __attribute__ ((__nothrow__));




extern double cosh (double __x) __attribute__ ((__nothrow__)); extern double __cosh (double __x) __attribute__ ((__nothrow__));

extern double sinh (double __x) __attribute__ ((__nothrow__)); extern double __sinh (double __x) __attribute__ ((__nothrow__));

extern double tanh (double __x) __attribute__ ((__nothrow__)); extern double __tanh (double __x) __attribute__ ((__nothrow__));

# 87 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/mathcalls.h" 3 4


extern double acosh (double __x) __attribute__ ((__nothrow__)); extern double __acosh (double __x) __attribute__ ((__nothrow__));

extern double asinh (double __x) __attribute__ ((__nothrow__)); extern double __asinh (double __x) __attribute__ ((__nothrow__));

extern double atanh (double __x) __attribute__ ((__nothrow__)); extern double __atanh (double __x) __attribute__ ((__nothrow__));







extern double exp (double __x) __attribute__ ((__nothrow__)); extern double __exp (double __x) __attribute__ ((__nothrow__));


extern double frexp (double __x, int *__exponent) __attribute__ ((__nothrow__)); extern double __frexp (double __x, int *__exponent) __attribute__ ((__nothrow__));


extern double ldexp (double __x, int __exponent) __attribute__ ((__nothrow__)); extern double __ldexp (double __x, int __exponent) __attribute__ ((__nothrow__));


extern double log (double __x) __attribute__ ((__nothrow__)); extern double __log (double __x) __attribute__ ((__nothrow__));


extern double log10 (double __x) __attribute__ ((__nothrow__)); extern double __log10 (double __x) __attribute__ ((__nothrow__));


extern double modf (double __x, double *__iptr) __attribute__ ((__nothrow__)); extern double __modf (double __x, double *__iptr) __attribute__ ((__nothrow__));

# 127 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/mathcalls.h" 3 4


extern double expm1 (double __x) __attribute__ ((__nothrow__)); extern double __expm1 (double __x) __attribute__ ((__nothrow__));


extern double log1p (double __x) __attribute__ ((__nothrow__)); extern double __log1p (double __x) __attribute__ ((__nothrow__));


extern double logb (double __x) __attribute__ ((__nothrow__)); extern double __logb (double __x) __attribute__ ((__nothrow__));






extern double exp2 (double __x) __attribute__ ((__nothrow__)); extern double __exp2 (double __x) __attribute__ ((__nothrow__));


extern double log2 (double __x) __attribute__ ((__nothrow__)); extern double __log2 (double __x) __attribute__ ((__nothrow__));








extern double pow (double __x, double __y) __attribute__ ((__nothrow__)); extern double __pow (double __x, double __y) __attribute__ ((__nothrow__));


extern double sqrt (double __x) __attribute__ ((__nothrow__)); extern double __sqrt (double __x) __attribute__ ((__nothrow__));





extern double hypot (double __x, double __y) __attribute__ ((__nothrow__)); extern double __hypot (double __x, double __y) __attribute__ ((__nothrow__));






extern double cbrt (double __x) __attribute__ ((__nothrow__)); extern double __cbrt (double __x) __attribute__ ((__nothrow__));








extern double ceil (double __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern double __ceil (double __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__));


extern double fabs (double __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern double __fabs (double __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__));


extern double floor (double __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern double __floor (double __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__));


extern double fmod (double __x, double __y) __attribute__ ((__nothrow__)); extern double __fmod (double __x, double __y) __attribute__ ((__nothrow__));




extern int __isinf (double __value) __attribute__ ((__nothrow__)) __attribute__ ((__const__));


extern int __finite (double __value) __attribute__ ((__nothrow__)) __attribute__ ((__const__));





extern int isinf (double __value) __attribute__ ((__nothrow__)) __attribute__ ((__const__));


extern int finite (double __value) __attribute__ ((__nothrow__)) __attribute__ ((__const__));


extern double drem (double __x, double __y) __attribute__ ((__nothrow__)); extern double __drem (double __x, double __y) __attribute__ ((__nothrow__));



extern double significand (double __x) __attribute__ ((__nothrow__)); extern double __significand (double __x) __attribute__ ((__nothrow__));





extern double copysign (double __x, double __y) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern double __copysign (double __x, double __y) __attribute__ ((__nothrow__)) __attribute__ ((__const__));






extern double nan (__const char *__tagb) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern double __nan (__const char *__tagb) __attribute__ ((__nothrow__)) __attribute__ ((__const__));





extern int __isnan (double __value) __attribute__ ((__nothrow__)) __attribute__ ((__const__));



extern int isnan (double __value) __attribute__ ((__nothrow__)) __attribute__ ((__const__));


extern double j0 (double) __attribute__ ((__nothrow__)); extern double __j0 (double) __attribute__ ((__nothrow__));
extern double j1 (double) __attribute__ ((__nothrow__)); extern double __j1 (double) __attribute__ ((__nothrow__));
extern double jn (int, double) __attribute__ ((__nothrow__)); extern double __jn (int, double) __attribute__ ((__nothrow__));
extern double y0 (double) __attribute__ ((__nothrow__)); extern double __y0 (double) __attribute__ ((__nothrow__));
extern double y1 (double) __attribute__ ((__nothrow__)); extern double __y1 (double) __attribute__ ((__nothrow__));
extern double yn (int, double) __attribute__ ((__nothrow__)); extern double __yn (int, double) __attribute__ ((__nothrow__));






extern double erf (double) __attribute__ ((__nothrow__)); extern double __erf (double) __attribute__ ((__nothrow__));
extern double erfc (double) __attribute__ ((__nothrow__)); extern double __erfc (double) __attribute__ ((__nothrow__));
extern double lgamma (double) __attribute__ ((__nothrow__)); extern double __lgamma (double) __attribute__ ((__nothrow__));






extern double tgamma (double) __attribute__ ((__nothrow__)); extern double __tgamma (double) __attribute__ ((__nothrow__));





extern double gamma (double) __attribute__ ((__nothrow__)); extern double __gamma (double) __attribute__ ((__nothrow__));






extern double lgamma_r (double, int *__signgamp) __attribute__ ((__nothrow__)); extern double __lgamma_r (double, int *__signgamp) __attribute__ ((__nothrow__));







extern double rint (double __x) __attribute__ ((__nothrow__)); extern double __rint (double __x) __attribute__ ((__nothrow__));


extern double nextafter (double __x, double __y) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern double __nextafter (double __x, double __y) __attribute__ ((__nothrow__)) __attribute__ ((__const__));

extern double nexttoward (double __x, long double __y) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern double __nexttoward (double __x, long double __y) __attribute__ ((__nothrow__)) __attribute__ ((__const__));



extern double remainder (double __x, double __y) __attribute__ ((__nothrow__)); extern double __remainder (double __x, double __y) __attribute__ ((__nothrow__));



extern double scalbn (double __x, int __n) __attribute__ ((__nothrow__)); extern double __scalbn (double __x, int __n) __attribute__ ((__nothrow__));



extern int ilogb (double __x) __attribute__ ((__nothrow__)); extern int __ilogb (double __x) __attribute__ ((__nothrow__));




extern double scalbln (double __x, long int __n) __attribute__ ((__nothrow__)); extern double __scalbln (double __x, long int __n) __attribute__ ((__nothrow__));



extern double nearbyint (double __x) __attribute__ ((__nothrow__)); extern double __nearbyint (double __x) __attribute__ ((__nothrow__));



extern double round (double __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern double __round (double __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__));



extern double trunc (double __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern double __trunc (double __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__));




extern double remquo (double __x, double __y, int *__quo) __attribute__ ((__nothrow__)); extern double __remquo (double __x, double __y, int *__quo) __attribute__ ((__nothrow__));






extern long int lrint (double __x) __attribute__ ((__nothrow__)); extern long int __lrint (double __x) __attribute__ ((__nothrow__));
extern long long int llrint (double __x) __attribute__ ((__nothrow__)); extern long long int __llrint (double __x) __attribute__ ((__nothrow__));



extern long int lround (double __x) __attribute__ ((__nothrow__)); extern long int __lround (double __x) __attribute__ ((__nothrow__));
extern long long int llround (double __x) __attribute__ ((__nothrow__)); extern long long int __llround (double __x) __attribute__ ((__nothrow__));



extern double fdim (double __x, double __y) __attribute__ ((__nothrow__)); extern double __fdim (double __x, double __y) __attribute__ ((__nothrow__));


extern double fmax (double __x, double __y) __attribute__ ((__nothrow__)); extern double __fmax (double __x, double __y) __attribute__ ((__nothrow__));


extern double fmin (double __x, double __y) __attribute__ ((__nothrow__)); extern double __fmin (double __x, double __y) __attribute__ ((__nothrow__));



extern int __fpclassify (double __value) __attribute__ ((__nothrow__))
     __attribute__ ((__const__));


extern int __signbit (double __value) __attribute__ ((__nothrow__))
     __attribute__ ((__const__));



extern double fma (double __x, double __y, double __z) __attribute__ ((__nothrow__)); extern double __fma (double __x, double __y, double __z) __attribute__ ((__nothrow__));








extern double scalb (double __x, double __n) __attribute__ ((__nothrow__)); extern double __scalb (double __x, double __n) __attribute__ ((__nothrow__));
# 72 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/math.h" 2 3 4
# 94 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/math.h" 3 4
# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/mathcalls.h" 1 3 4
# 53 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/mathcalls.h" 3 4


extern float acosf (float __x) __attribute__ ((__nothrow__)); extern float __acosf (float __x) __attribute__ ((__nothrow__));

extern float asinf (float __x) __attribute__ ((__nothrow__)); extern float __asinf (float __x) __attribute__ ((__nothrow__));

extern float atanf (float __x) __attribute__ ((__nothrow__)); extern float __atanf (float __x) __attribute__ ((__nothrow__));

extern float atan2f (float __y, float __x) __attribute__ ((__nothrow__)); extern float __atan2f (float __y, float __x) __attribute__ ((__nothrow__));


extern float cosf (float __x) __attribute__ ((__nothrow__)); extern float __cosf (float __x) __attribute__ ((__nothrow__));

extern float sinf (float __x) __attribute__ ((__nothrow__)); extern float __sinf (float __x) __attribute__ ((__nothrow__));

extern float tanf (float __x) __attribute__ ((__nothrow__)); extern float __tanf (float __x) __attribute__ ((__nothrow__));




extern float coshf (float __x) __attribute__ ((__nothrow__)); extern float __coshf (float __x) __attribute__ ((__nothrow__));

extern float sinhf (float __x) __attribute__ ((__nothrow__)); extern float __sinhf (float __x) __attribute__ ((__nothrow__));

extern float tanhf (float __x) __attribute__ ((__nothrow__)); extern float __tanhf (float __x) __attribute__ ((__nothrow__));

# 87 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/mathcalls.h" 3 4


extern float acoshf (float __x) __attribute__ ((__nothrow__)); extern float __acoshf (float __x) __attribute__ ((__nothrow__));

extern float asinhf (float __x) __attribute__ ((__nothrow__)); extern float __asinhf (float __x) __attribute__ ((__nothrow__));

extern float atanhf (float __x) __attribute__ ((__nothrow__)); extern float __atanhf (float __x) __attribute__ ((__nothrow__));







extern float expf (float __x) __attribute__ ((__nothrow__)); extern float __expf (float __x) __attribute__ ((__nothrow__));


extern float frexpf (float __x, int *__exponent) __attribute__ ((__nothrow__)); extern float __frexpf (float __x, int *__exponent) __attribute__ ((__nothrow__));


extern float ldexpf (float __x, int __exponent) __attribute__ ((__nothrow__)); extern float __ldexpf (float __x, int __exponent) __attribute__ ((__nothrow__));


extern float logf (float __x) __attribute__ ((__nothrow__)); extern float __logf (float __x) __attribute__ ((__nothrow__));


extern float log10f (float __x) __attribute__ ((__nothrow__)); extern float __log10f (float __x) __attribute__ ((__nothrow__));


extern float modff (float __x, float *__iptr) __attribute__ ((__nothrow__)); extern float __modff (float __x, float *__iptr) __attribute__ ((__nothrow__));

# 127 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/mathcalls.h" 3 4


extern float expm1f (float __x) __attribute__ ((__nothrow__)); extern float __expm1f (float __x) __attribute__ ((__nothrow__));


extern float log1pf (float __x) __attribute__ ((__nothrow__)); extern float __log1pf (float __x) __attribute__ ((__nothrow__));


extern float logbf (float __x) __attribute__ ((__nothrow__)); extern float __logbf (float __x) __attribute__ ((__nothrow__));






extern float exp2f (float __x) __attribute__ ((__nothrow__)); extern float __exp2f (float __x) __attribute__ ((__nothrow__));


extern float log2f (float __x) __attribute__ ((__nothrow__)); extern float __log2f (float __x) __attribute__ ((__nothrow__));








extern float powf (float __x, float __y) __attribute__ ((__nothrow__)); extern float __powf (float __x, float __y) __attribute__ ((__nothrow__));


extern float sqrtf (float __x) __attribute__ ((__nothrow__)); extern float __sqrtf (float __x) __attribute__ ((__nothrow__));





extern float hypotf (float __x, float __y) __attribute__ ((__nothrow__)); extern float __hypotf (float __x, float __y) __attribute__ ((__nothrow__));






extern float cbrtf (float __x) __attribute__ ((__nothrow__)); extern float __cbrtf (float __x) __attribute__ ((__nothrow__));








extern float ceilf (float __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern float __ceilf (float __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__));


extern float fabsf (float __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern float __fabsf (float __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__));


extern float floorf (float __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern float __floorf (float __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__));


extern float fmodf (float __x, float __y) __attribute__ ((__nothrow__)); extern float __fmodf (float __x, float __y) __attribute__ ((__nothrow__));




extern int __isinff (float __value) __attribute__ ((__nothrow__)) __attribute__ ((__const__));


extern int __finitef (float __value) __attribute__ ((__nothrow__)) __attribute__ ((__const__));





extern int isinff (float __value) __attribute__ ((__nothrow__)) __attribute__ ((__const__));


extern int finitef (float __value) __attribute__ ((__nothrow__)) __attribute__ ((__const__));


extern float dremf (float __x, float __y) __attribute__ ((__nothrow__)); extern float __dremf (float __x, float __y) __attribute__ ((__nothrow__));



extern float significandf (float __x) __attribute__ ((__nothrow__)); extern float __significandf (float __x) __attribute__ ((__nothrow__));





extern float copysignf (float __x, float __y) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern float __copysignf (float __x, float __y) __attribute__ ((__nothrow__)) __attribute__ ((__const__));






extern float nanf (__const char *__tagb) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern float __nanf (__const char *__tagb) __attribute__ ((__nothrow__)) __attribute__ ((__const__));





extern int __isnanf (float __value) __attribute__ ((__nothrow__)) __attribute__ ((__const__));



extern int isnanf (float __value) __attribute__ ((__nothrow__)) __attribute__ ((__const__));


extern float j0f (float) __attribute__ ((__nothrow__)); extern float __j0f (float) __attribute__ ((__nothrow__));
extern float j1f (float) __attribute__ ((__nothrow__)); extern float __j1f (float) __attribute__ ((__nothrow__));
extern float jnf (int, float) __attribute__ ((__nothrow__)); extern float __jnf (int, float) __attribute__ ((__nothrow__));
extern float y0f (float) __attribute__ ((__nothrow__)); extern float __y0f (float) __attribute__ ((__nothrow__));
extern float y1f (float) __attribute__ ((__nothrow__)); extern float __y1f (float) __attribute__ ((__nothrow__));
extern float ynf (int, float) __attribute__ ((__nothrow__)); extern float __ynf (int, float) __attribute__ ((__nothrow__));






extern float erff (float) __attribute__ ((__nothrow__)); extern float __erff (float) __attribute__ ((__nothrow__));
extern float erfcf (float) __attribute__ ((__nothrow__)); extern float __erfcf (float) __attribute__ ((__nothrow__));
extern float lgammaf (float) __attribute__ ((__nothrow__)); extern float __lgammaf (float) __attribute__ ((__nothrow__));






extern float tgammaf (float) __attribute__ ((__nothrow__)); extern float __tgammaf (float) __attribute__ ((__nothrow__));





extern float gammaf (float) __attribute__ ((__nothrow__)); extern float __gammaf (float) __attribute__ ((__nothrow__));






extern float lgammaf_r (float, int *__signgamp) __attribute__ ((__nothrow__)); extern float __lgammaf_r (float, int *__signgamp) __attribute__ ((__nothrow__));







extern float rintf (float __x) __attribute__ ((__nothrow__)); extern float __rintf (float __x) __attribute__ ((__nothrow__));


extern float nextafterf (float __x, float __y) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern float __nextafterf (float __x, float __y) __attribute__ ((__nothrow__)) __attribute__ ((__const__));

extern float nexttowardf (float __x, long double __y) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern float __nexttowardf (float __x, long double __y) __attribute__ ((__nothrow__)) __attribute__ ((__const__));



extern float remainderf (float __x, float __y) __attribute__ ((__nothrow__)); extern float __remainderf (float __x, float __y) __attribute__ ((__nothrow__));



extern float scalbnf (float __x, int __n) __attribute__ ((__nothrow__)); extern float __scalbnf (float __x, int __n) __attribute__ ((__nothrow__));



extern int ilogbf (float __x) __attribute__ ((__nothrow__)); extern int __ilogbf (float __x) __attribute__ ((__nothrow__));




extern float scalblnf (float __x, long int __n) __attribute__ ((__nothrow__)); extern float __scalblnf (float __x, long int __n) __attribute__ ((__nothrow__));



extern float nearbyintf (float __x) __attribute__ ((__nothrow__)); extern float __nearbyintf (float __x) __attribute__ ((__nothrow__));



extern float roundf (float __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern float __roundf (float __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__));



extern float truncf (float __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern float __truncf (float __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__));




extern float remquof (float __x, float __y, int *__quo) __attribute__ ((__nothrow__)); extern float __remquof (float __x, float __y, int *__quo) __attribute__ ((__nothrow__));






extern long int lrintf (float __x) __attribute__ ((__nothrow__)); extern long int __lrintf (float __x) __attribute__ ((__nothrow__));
extern long long int llrintf (float __x) __attribute__ ((__nothrow__)); extern long long int __llrintf (float __x) __attribute__ ((__nothrow__));



extern long int lroundf (float __x) __attribute__ ((__nothrow__)); extern long int __lroundf (float __x) __attribute__ ((__nothrow__));
extern long long int llroundf (float __x) __attribute__ ((__nothrow__)); extern long long int __llroundf (float __x) __attribute__ ((__nothrow__));



extern float fdimf (float __x, float __y) __attribute__ ((__nothrow__)); extern float __fdimf (float __x, float __y) __attribute__ ((__nothrow__));


extern float fmaxf (float __x, float __y) __attribute__ ((__nothrow__)); extern float __fmaxf (float __x, float __y) __attribute__ ((__nothrow__));


extern float fminf (float __x, float __y) __attribute__ ((__nothrow__)); extern float __fminf (float __x, float __y) __attribute__ ((__nothrow__));



extern int __fpclassifyf (float __value) __attribute__ ((__nothrow__))
     __attribute__ ((__const__));


extern int __signbitf (float __value) __attribute__ ((__nothrow__))
     __attribute__ ((__const__));



extern float fmaf (float __x, float __y, float __z) __attribute__ ((__nothrow__)); extern float __fmaf (float __x, float __y, float __z) __attribute__ ((__nothrow__));








extern float scalbf (float __x, float __n) __attribute__ ((__nothrow__)); extern float __scalbf (float __x, float __n) __attribute__ ((__nothrow__));
# 95 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/math.h" 2 3 4
# 145 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/math.h" 3 4
# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/mathcalls.h" 1 3 4
# 53 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/mathcalls.h" 3 4


extern long double acosl (long double __x) __asm__ ("" "acos") __attribute__ ((__nothrow__)); extern long double __acosl (long double __x) __asm__ ("" "__acos") __attribute__ ((__nothrow__));

extern long double asinl (long double __x) __asm__ ("" "asin") __attribute__ ((__nothrow__)); extern long double __asinl (long double __x) __asm__ ("" "__asin") __attribute__ ((__nothrow__));

extern long double atanl (long double __x) __asm__ ("" "atan") __attribute__ ((__nothrow__)); extern long double __atanl (long double __x) __asm__ ("" "__atan") __attribute__ ((__nothrow__));

extern long double atan2l (long double __y, long double __x) __asm__ ("" "atan2") __attribute__ ((__nothrow__)); extern long double __atan2l (long double __y, long double __x) __asm__ ("" "__atan2") __attribute__ ((__nothrow__));


extern long double cosl (long double __x) __asm__ ("" "cos") __attribute__ ((__nothrow__)); extern long double __cosl (long double __x) __asm__ ("" "__cos") __attribute__ ((__nothrow__));

extern long double sinl (long double __x) __asm__ ("" "sin") __attribute__ ((__nothrow__)); extern long double __sinl (long double __x) __asm__ ("" "__sin") __attribute__ ((__nothrow__));

extern long double tanl (long double __x) __asm__ ("" "tan") __attribute__ ((__nothrow__)); extern long double __tanl (long double __x) __asm__ ("" "__tan") __attribute__ ((__nothrow__));




extern long double coshl (long double __x) __asm__ ("" "cosh") __attribute__ ((__nothrow__)); extern long double __coshl (long double __x) __asm__ ("" "__cosh") __attribute__ ((__nothrow__));

extern long double sinhl (long double __x) __asm__ ("" "sinh") __attribute__ ((__nothrow__)); extern long double __sinhl (long double __x) __asm__ ("" "__sinh") __attribute__ ((__nothrow__));

extern long double tanhl (long double __x) __asm__ ("" "tanh") __attribute__ ((__nothrow__)); extern long double __tanhl (long double __x) __asm__ ("" "__tanh") __attribute__ ((__nothrow__));

# 87 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/mathcalls.h" 3 4


extern long double acoshl (long double __x) __asm__ ("" "acosh") __attribute__ ((__nothrow__)); extern long double __acoshl (long double __x) __asm__ ("" "__acosh") __attribute__ ((__nothrow__));

extern long double asinhl (long double __x) __asm__ ("" "asinh") __attribute__ ((__nothrow__)); extern long double __asinhl (long double __x) __asm__ ("" "__asinh") __attribute__ ((__nothrow__));

extern long double atanhl (long double __x) __asm__ ("" "atanh") __attribute__ ((__nothrow__)); extern long double __atanhl (long double __x) __asm__ ("" "__atanh") __attribute__ ((__nothrow__));







extern long double expl (long double __x) __asm__ ("" "exp") __attribute__ ((__nothrow__)); extern long double __expl (long double __x) __asm__ ("" "__exp") __attribute__ ((__nothrow__));


extern long double frexpl (long double __x, int *__exponent) __asm__ ("" "frexp") __attribute__ ((__nothrow__)); extern long double __frexpl (long double __x, int *__exponent) __asm__ ("" "__frexp") __attribute__ ((__nothrow__));


extern long double ldexpl (long double __x, int __exponent) __asm__ ("" "ldexp") __attribute__ ((__nothrow__)); extern long double __ldexpl (long double __x, int __exponent) __asm__ ("" "__ldexp") __attribute__ ((__nothrow__));


extern long double logl (long double __x) __asm__ ("" "log") __attribute__ ((__nothrow__)); extern long double __logl (long double __x) __asm__ ("" "__log") __attribute__ ((__nothrow__));


extern long double log10l (long double __x) __asm__ ("" "log10") __attribute__ ((__nothrow__)); extern long double __log10l (long double __x) __asm__ ("" "__log10") __attribute__ ((__nothrow__));


extern long double modfl (long double __x, long double *__iptr) __asm__ ("" "modf") __attribute__ ((__nothrow__)); extern long double __modfl (long double __x, long double *__iptr) __asm__ ("" "__modf") __attribute__ ((__nothrow__));

# 127 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/mathcalls.h" 3 4


extern long double expm1l (long double __x) __asm__ ("" "expm1") __attribute__ ((__nothrow__)); extern long double __expm1l (long double __x) __asm__ ("" "__expm1") __attribute__ ((__nothrow__));


extern long double log1pl (long double __x) __asm__ ("" "log1p") __attribute__ ((__nothrow__)); extern long double __log1pl (long double __x) __asm__ ("" "__log1p") __attribute__ ((__nothrow__));


extern long double logbl (long double __x) __asm__ ("" "logb") __attribute__ ((__nothrow__)); extern long double __logbl (long double __x) __asm__ ("" "__logb") __attribute__ ((__nothrow__));






extern long double exp2l (long double __x) __asm__ ("" "exp2") __attribute__ ((__nothrow__)); extern long double __exp2l (long double __x) __asm__ ("" "__exp2") __attribute__ ((__nothrow__));


extern long double log2l (long double __x) __asm__ ("" "log2") __attribute__ ((__nothrow__)); extern long double __log2l (long double __x) __asm__ ("" "__log2") __attribute__ ((__nothrow__));








extern long double powl (long double __x, long double __y) __asm__ ("" "pow") __attribute__ ((__nothrow__)); extern long double __powl (long double __x, long double __y) __asm__ ("" "__pow") __attribute__ ((__nothrow__));


extern long double sqrtl (long double __x) __asm__ ("" "sqrt") __attribute__ ((__nothrow__)); extern long double __sqrtl (long double __x) __asm__ ("" "__sqrt") __attribute__ ((__nothrow__));





extern long double hypotl (long double __x, long double __y) __asm__ ("" "hypot") __attribute__ ((__nothrow__)); extern long double __hypotl (long double __x, long double __y) __asm__ ("" "__hypot") __attribute__ ((__nothrow__));






extern long double cbrtl (long double __x) __asm__ ("" "cbrt") __attribute__ ((__nothrow__)); extern long double __cbrtl (long double __x) __asm__ ("" "__cbrt") __attribute__ ((__nothrow__));








extern long double ceill (long double __x) __asm__ ("" "ceil") __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern long double __ceill (long double __x) __asm__ ("" "__ceil") __attribute__ ((__nothrow__)) __attribute__ ((__const__));


extern long double fabsl (long double __x) __asm__ ("" "fabs") __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern long double __fabsl (long double __x) __asm__ ("" "__fabs") __attribute__ ((__nothrow__)) __attribute__ ((__const__));


extern long double floorl (long double __x) __asm__ ("" "floor") __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern long double __floorl (long double __x) __asm__ ("" "__floor") __attribute__ ((__nothrow__)) __attribute__ ((__const__));


extern long double fmodl (long double __x, long double __y) __asm__ ("" "fmod") __attribute__ ((__nothrow__)); extern long double __fmodl (long double __x, long double __y) __asm__ ("" "__fmod") __attribute__ ((__nothrow__));




extern int __isinfl (long double __value) __asm__ ("" "__isinf") __attribute__ ((__nothrow__)) __attribute__ ((__const__));


extern int __finitel (long double __value) __asm__ ("" "__finite") __attribute__ ((__nothrow__)) __attribute__ ((__const__));





extern int isinfl (long double __value) __asm__ ("" "isinf") __attribute__ ((__nothrow__)) __attribute__ ((__const__));


extern int finitel (long double __value) __asm__ ("" "finite") __attribute__ ((__nothrow__)) __attribute__ ((__const__));


extern long double dreml (long double __x, long double __y) __asm__ ("" "drem") __attribute__ ((__nothrow__)); extern long double __dreml (long double __x, long double __y) __asm__ ("" "__drem") __attribute__ ((__nothrow__));



extern long double significandl (long double __x) __asm__ ("" "significand") __attribute__ ((__nothrow__)); extern long double __significandl (long double __x) __asm__ ("" "__significand") __attribute__ ((__nothrow__));





extern long double copysignl (long double __x, long double __y) __asm__ ("" "copysign") __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern long double __copysignl (long double __x, long double __y) __asm__ ("" "__copysign") __attribute__ ((__nothrow__)) __attribute__ ((__const__));






extern long double nanl (__const char *__tagb) __asm__ ("" "nan") __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern long double __nanl (__const char *__tagb) __asm__ ("" "__nan") __attribute__ ((__nothrow__)) __attribute__ ((__const__));





extern int __isnanl (long double __value) __asm__ ("" "__isnan") __attribute__ ((__nothrow__)) __attribute__ ((__const__));



extern int isnanl (long double __value) __asm__ ("" "isnan") __attribute__ ((__nothrow__)) __attribute__ ((__const__));


extern long double j0l (long double) __asm__ ("" "j0") __attribute__ ((__nothrow__)); extern long double __j0l (long double) __asm__ ("" "__j0") __attribute__ ((__nothrow__));
extern long double j1l (long double) __asm__ ("" "j1") __attribute__ ((__nothrow__)); extern long double __j1l (long double) __asm__ ("" "__j1") __attribute__ ((__nothrow__));
extern long double jnl (int, long double) __asm__ ("" "jn") __attribute__ ((__nothrow__)); extern long double __jnl (int, long double) __asm__ ("" "__jn") __attribute__ ((__nothrow__));
extern long double y0l (long double) __asm__ ("" "y0") __attribute__ ((__nothrow__)); extern long double __y0l (long double) __asm__ ("" "__y0") __attribute__ ((__nothrow__));
extern long double y1l (long double) __asm__ ("" "y1") __attribute__ ((__nothrow__)); extern long double __y1l (long double) __asm__ ("" "__y1") __attribute__ ((__nothrow__));
extern long double ynl (int, long double) __asm__ ("" "yn") __attribute__ ((__nothrow__)); extern long double __ynl (int, long double) __asm__ ("" "__yn") __attribute__ ((__nothrow__));






extern long double erfl (long double) __asm__ ("" "erf") __attribute__ ((__nothrow__)); extern long double __erfl (long double) __asm__ ("" "__erf") __attribute__ ((__nothrow__));
extern long double erfcl (long double) __asm__ ("" "erfc") __attribute__ ((__nothrow__)); extern long double __erfcl (long double) __asm__ ("" "__erfc") __attribute__ ((__nothrow__));
extern long double lgammal (long double) __asm__ ("" "lgamma") __attribute__ ((__nothrow__)); extern long double __lgammal (long double) __asm__ ("" "__lgamma") __attribute__ ((__nothrow__));






extern long double tgammal (long double) __asm__ ("" "tgamma") __attribute__ ((__nothrow__)); extern long double __tgammal (long double) __asm__ ("" "__tgamma") __attribute__ ((__nothrow__));





extern long double gammal (long double) __asm__ ("" "gamma") __attribute__ ((__nothrow__)); extern long double __gammal (long double) __asm__ ("" "__gamma") __attribute__ ((__nothrow__));






extern long double lgammal_r (long double, int *__signgamp) __asm__ ("" "lgamma_r") __attribute__ ((__nothrow__)); extern long double __lgammal_r (long double, int *__signgamp) __asm__ ("" "__lgamma_r") __attribute__ ((__nothrow__));







extern long double rintl (long double __x) __asm__ ("" "rint") __attribute__ ((__nothrow__)); extern long double __rintl (long double __x) __asm__ ("" "__rint") __attribute__ ((__nothrow__));


extern long double nextafterl (long double __x, long double __y) __asm__ ("" "nextafter") __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern long double __nextafterl (long double __x, long double __y) __asm__ ("" "__nextafter") __attribute__ ((__nothrow__)) __attribute__ ((__const__));

extern long double nexttowardl (long double __x, long double __y) __asm__ ("" "nexttoward") __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern long double __nexttowardl (long double __x, long double __y) __asm__ ("" "__nexttoward") __attribute__ ((__nothrow__)) __attribute__ ((__const__));



extern long double remainderl (long double __x, long double __y) __asm__ ("" "remainder") __attribute__ ((__nothrow__)); extern long double __remainderl (long double __x, long double __y) __asm__ ("" "__remainder") __attribute__ ((__nothrow__));



extern long double scalbnl (long double __x, int __n) __asm__ ("" "scalbn") __attribute__ ((__nothrow__)); extern long double __scalbnl (long double __x, int __n) __asm__ ("" "__scalbn") __attribute__ ((__nothrow__));



extern int ilogbl (long double __x) __asm__ ("" "ilogb") __attribute__ ((__nothrow__)); extern int __ilogbl (long double __x) __asm__ ("" "__ilogb") __attribute__ ((__nothrow__));




extern long double scalblnl (long double __x, long int __n) __asm__ ("" "scalbln") __attribute__ ((__nothrow__)); extern long double __scalblnl (long double __x, long int __n) __asm__ ("" "__scalbln") __attribute__ ((__nothrow__));



extern long double nearbyintl (long double __x) __asm__ ("" "nearbyint") __attribute__ ((__nothrow__)); extern long double __nearbyintl (long double __x) __asm__ ("" "__nearbyint") __attribute__ ((__nothrow__));



extern long double roundl (long double __x) __asm__ ("" "round") __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern long double __roundl (long double __x) __asm__ ("" "__round") __attribute__ ((__nothrow__)) __attribute__ ((__const__));



extern long double truncl (long double __x) __asm__ ("" "trunc") __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern long double __truncl (long double __x) __asm__ ("" "__trunc") __attribute__ ((__nothrow__)) __attribute__ ((__const__));




extern long double remquol (long double __x, long double __y, int *__quo) __asm__ ("" "remquo") __attribute__ ((__nothrow__)); extern long double __remquol (long double __x, long double __y, int *__quo) __asm__ ("" "__remquo") __attribute__ ((__nothrow__));






extern long int lrintl (long double __x) __asm__ ("" "lrint") __attribute__ ((__nothrow__)); extern long int __lrintl (long double __x) __asm__ ("" "__lrint") __attribute__ ((__nothrow__));
extern long long int llrintl (long double __x) __asm__ ("" "llrint") __attribute__ ((__nothrow__)); extern long long int __llrintl (long double __x) __asm__ ("" "__llrint") __attribute__ ((__nothrow__));



extern long int lroundl (long double __x) __asm__ ("" "lround") __attribute__ ((__nothrow__)); extern long int __lroundl (long double __x) __asm__ ("" "__lround") __attribute__ ((__nothrow__));
extern long long int llroundl (long double __x) __asm__ ("" "llround") __attribute__ ((__nothrow__)); extern long long int __llroundl (long double __x) __asm__ ("" "__llround") __attribute__ ((__nothrow__));



extern long double fdiml (long double __x, long double __y) __asm__ ("" "fdim") __attribute__ ((__nothrow__)); extern long double __fdiml (long double __x, long double __y) __asm__ ("" "__fdim") __attribute__ ((__nothrow__));


extern long double fmaxl (long double __x, long double __y) __asm__ ("" "fmax") __attribute__ ((__nothrow__)); extern long double __fmaxl (long double __x, long double __y) __asm__ ("" "__fmax") __attribute__ ((__nothrow__));


extern long double fminl (long double __x, long double __y) __asm__ ("" "fmin") __attribute__ ((__nothrow__)); extern long double __fminl (long double __x, long double __y) __asm__ ("" "__fmin") __attribute__ ((__nothrow__));



extern int __fpclassifyl (long double __value) __asm__ ("" "__fpclassify") __attribute__ ((__nothrow__))
     __attribute__ ((__const__));


extern int __signbitl (long double __value) __asm__ ("" "__signbit") __attribute__ ((__nothrow__))
     __attribute__ ((__const__));



extern long double fmal (long double __x, long double __y, long double __z) __asm__ ("" "fma") __attribute__ ((__nothrow__)); extern long double __fmal (long double __x, long double __y, long double __z) __asm__ ("" "__fma") __attribute__ ((__nothrow__));








extern long double scalbl (long double __x, long double __n) __asm__ ("" "scalb") __attribute__ ((__nothrow__)); extern long double __scalbl (long double __x, long double __n) __asm__ ("" "__scalb") __attribute__ ((__nothrow__));
# 146 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/math.h" 2 3 4
# 161 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/math.h" 3 4
extern int signgam;
# 202 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/math.h" 3 4
enum
  {
    FP_NAN,

    FP_INFINITE,

    FP_ZERO,

    FP_SUBNORMAL,

    FP_NORMAL

  };
# 295 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/math.h" 3 4
typedef enum
{
  _IEEE_ = -1,
  _SVID_,
  _XOPEN_,
  _POSIX_,
  _ISOC_
} _LIB_VERSION_TYPE;




extern _LIB_VERSION_TYPE _LIB_VERSION;
# 320 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/math.h" 3 4
struct exception

  {
    int type;
    char *name;
    double arg1;
    double arg2;
    double retval;
  };




extern int matherr (struct exception *__exc);
# 476 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/math.h" 3 4

# 51 "macros.h" 2
# 37 "scissor.c" 2
# 1 "dlist.h" 1
# 46 "dlist.h"
extern void gl_init_lists( void );

extern GLint gl_list_index( void );




extern void gl_CallList( GLcontext *ctx, GLuint list );

extern void gl_CallLists( GLcontext *ctx,
                          GLsizei n, GLenum type, const GLvoid *lists );

extern void gl_DeleteLists( GLcontext *ctx, GLuint list, GLsizei range );

extern void gl_EndList( GLcontext *ctx );

extern GLuint gl_GenLists( GLcontext *ctx, GLsizei range );

extern GLboolean gl_IsList( GLcontext *ctx, GLuint list );

extern void gl_ListBase( GLcontext *ctx, GLuint base );

extern void gl_NewList( GLcontext *ctx, GLuint list, GLenum mode );




extern void gl_save_Accum( GLcontext *ctx, GLenum op, GLfloat value );

extern void gl_save_AlphaFunc( GLcontext *ctx, GLenum func, GLclampf ref );

extern void gl_save_BlendColor( GLcontext *ctx, GLfloat red, GLfloat green,
                                GLfloat blue, GLfloat alpha );

extern void gl_save_BlendEquation( GLcontext *ctx, GLenum mode );

extern void gl_save_BlendFunc( GLcontext *ctx,
                               GLenum sfactor, GLenum dfactor );

extern void gl_save_Begin( GLcontext *ctx, GLenum mode );

extern void gl_save_BindTexture( GLcontext *ctx,
                                 GLenum target, GLuint texture );

extern void gl_save_Bitmap( GLcontext *ctx, GLsizei width, GLsizei height,
       GLfloat xorig, GLfloat yorig,
       GLfloat xmove, GLfloat ymove,
                            const struct gl_image *bitmap );

extern void gl_save_CallList( GLcontext *ctx, GLuint list );

extern void gl_save_CallLists( GLcontext *ctx,
                               GLsizei n, GLenum type, const GLvoid *lists );

extern void gl_save_Clear( GLcontext *ctx, GLbitfield mask );

extern void gl_save_ClearAccum( GLcontext *ctx, GLfloat red, GLfloat green,
           GLfloat blue, GLfloat alpha );

extern void gl_save_ClearColor( GLcontext *ctx, GLclampf red, GLclampf green,
           GLclampf blue, GLclampf alpha );

extern void gl_save_ClearDepth( GLcontext *ctx, GLclampd depth );

extern void gl_save_ClearIndex( GLcontext *ctx, GLfloat c );

extern void gl_save_ClearStencil( GLcontext *ctx, GLint s );

extern void gl_save_ClipPlane( GLcontext *ctx,
                               GLenum plane, const GLfloat *equ );

extern void gl_save_Color4f( GLcontext *ctx, GLfloat r, GLfloat g,
                             GLfloat b, GLfloat a );

extern void gl_save_Color4ub( GLcontext *ctx, GLubyte r, GLubyte g,
                              GLubyte b, GLubyte a );

extern void gl_save_ColorMask( GLcontext *ctx, GLboolean red, GLboolean green,
          GLboolean blue, GLboolean alpha );

extern void gl_save_ColorMaterial( GLcontext *ctx, GLenum face, GLenum mode );

extern void gl_save_CopyPixels( GLcontext *ctx, GLint x, GLint y,
    GLsizei width, GLsizei height, GLenum type );

extern void gl_save_CopyTexImage1D( GLcontext *ctx,
                                    GLenum target, GLint level,
                                    GLenum internalformat,
                                    GLint x, GLint y, GLsizei width,
                                    GLint border );

extern void gl_save_CopyTexImage2D( GLcontext *ctx,
                                    GLenum target, GLint level,
                                    GLenum internalformat,
                                    GLint x, GLint y, GLsizei width,
                                    GLsizei height, GLint border );

extern void gl_save_CopyTexSubImage1D( GLcontext *ctx,
                                       GLenum target, GLint level,
                                       GLint xoffset, GLint x, GLint y,
                                       GLsizei width );

extern void gl_save_CopyTexSubImage2D( GLcontext *ctx,
                                       GLenum target, GLint level,
                                       GLint xoffset, GLint yoffset,
                                       GLint x, GLint y,
                                       GLsizei width, GLint height );

extern void gl_save_CopyTexSubImage3DEXT( GLcontext *ctx,
                                          GLenum target, GLint level,
                                          GLint xoffset, GLint yoffset, GLint zoffset,
                                          GLint x, GLint y,
                                          GLsizei width, GLint height );

extern void gl_save_CullFace( GLcontext *ctx, GLenum mode );

extern void gl_save_DepthFunc( GLcontext *ctx, GLenum func );

extern void gl_save_DepthMask( GLcontext *ctx, GLboolean mask );

extern void gl_save_DepthRange( GLcontext *ctx,
                                GLclampd nearval, GLclampd farval );

extern void gl_save_Disable( GLcontext *ctx, GLenum cap );

extern void gl_save_DrawBuffer( GLcontext *ctx, GLenum mode );

extern void gl_save_DrawPixels( GLcontext *ctx,
                                GLsizei width, GLsizei height, GLenum format,
           GLenum type, const GLvoid *pixels );

extern void gl_save_EdgeFlag( GLcontext *ctx, GLboolean flag );

extern void gl_save_Enable( GLcontext *ctx, GLenum cap );

extern void gl_save_End( GLcontext *ctx );

extern void gl_save_EvalCoord1f( GLcontext *ctx, GLfloat u );

extern void gl_save_EvalCoord2f( GLcontext *ctx, GLfloat u, GLfloat v );

extern void gl_save_EvalMesh1( GLcontext *ctx,
                               GLenum mode, GLint i1, GLint i2 );

extern void gl_save_EvalMesh2( GLcontext *ctx, GLenum mode, GLint i1, GLint i2,
          GLint j1, GLint j2 );

extern void gl_save_EvalPoint1( GLcontext *ctx, GLint i );

extern void gl_save_EvalPoint2( GLcontext *ctx, GLint i, GLint j );

extern void gl_save_Fogfv( GLcontext *ctx,
                           GLenum pname, const GLfloat *params );

extern void gl_save_FrontFace( GLcontext *ctx, GLenum mode );

extern void gl_save_Frustum( GLcontext *ctx, GLdouble left, GLdouble right,
                             GLdouble bottom, GLdouble top,
                             GLdouble nearval, GLdouble farval );

extern void gl_save_Hint( GLcontext *ctx, GLenum target, GLenum mode );

extern void gl_save_Indexf( GLcontext *ctx, GLfloat index );

extern void gl_save_Indexi( GLcontext *ctx, GLint index );

extern void gl_save_IndexMask( GLcontext *ctx, GLuint mask );

extern void gl_save_InitNames( GLcontext *ctx );

extern void gl_save_Lightfv( GLcontext *ctx, GLenum light, GLenum pname,
                             const GLfloat *params, GLint numparams );

extern void gl_save_LightModelfv( GLcontext *ctx, GLenum pname,
                                const GLfloat *params );

extern void gl_save_LineWidth( GLcontext *ctx, GLfloat width );

extern void gl_save_LineStipple( GLcontext *ctx, GLint factor,
                                 GLushort pattern );

extern void gl_save_ListBase( GLcontext *ctx, GLuint base );

extern void gl_save_LoadMatrixf( GLcontext *ctx, const GLfloat *m );

extern void gl_save_LoadName( GLcontext *ctx, GLuint name );

extern void gl_save_LogicOp( GLcontext *ctx, GLenum opcode );

extern void gl_save_Map1f( GLcontext *ctx, GLenum target,
                           GLfloat u1, GLfloat u2, GLint stride,
      GLint order, const GLfloat *points,
                           GLboolean retain );

extern void gl_save_Map2f( GLcontext *ctx, GLenum target,
      GLfloat u1, GLfloat u2, GLint ustride, GLint uorder,
      GLfloat v1, GLfloat v2, GLint vstride, GLint vorder,
      const GLfloat *points,
                           GLboolean retain );

extern void gl_save_MapGrid1f( GLcontext *ctx, GLint un,
                               GLfloat u1, GLfloat u2 );

extern void gl_save_MapGrid2f( GLcontext *ctx,
                               GLint un, GLfloat u1, GLfloat u2,
                               GLint vn, GLfloat v1, GLfloat v2 );

extern void gl_save_Materialfv( GLcontext *ctx, GLenum face, GLenum pname,
                                const GLfloat *params );

extern void gl_save_MatrixMode( GLcontext *ctx, GLenum mode );

extern void gl_save_MultMatrixf( GLcontext *ctx, const GLfloat *m );

extern void gl_save_NewList( GLcontext *ctx, GLuint list, GLenum mode );

extern void gl_save_Normal3fv( GLcontext *ctx, const GLfloat n[3] );

extern void gl_save_Normal3f( GLcontext *ctx,
                              GLfloat nx, GLfloat ny, GLfloat nz );

extern void gl_save_PassThrough( GLcontext *ctx, GLfloat token );

extern void gl_save_PixelMapfv( GLcontext *ctx, GLenum map, GLint mapsize,
                                const GLfloat *values );

extern void gl_save_PixelTransferf( GLcontext *ctx,
                                    GLenum pname, GLfloat param );

extern void gl_save_PixelZoom( GLcontext *ctx,
                               GLfloat xfactor, GLfloat yfactor );

extern void gl_save_PointSize( GLcontext *ctx, GLfloat size );

extern void gl_save_PolygonMode( GLcontext *ctx, GLenum face, GLenum mode );

extern void gl_save_PolygonStipple( GLcontext *ctx, const GLubyte *mask );

extern void gl_save_PolygonOffset( GLcontext *ctx,
                                   GLfloat factor, GLfloat units );

extern void gl_save_PopAttrib( GLcontext *ctx );

extern void gl_save_PopMatrix( GLcontext *ctx );

extern void gl_save_PopName( GLcontext *ctx );

extern void gl_save_PrioritizeTextures( GLcontext *ctx,
                                        GLsizei n, const GLuint *textures,
                                        const GLclampf *priorities );

extern void gl_save_PushAttrib( GLcontext *ctx, GLbitfield mask );

extern void gl_save_PushMatrix( GLcontext *ctx );

extern void gl_save_PushName( GLcontext *ctx, GLuint name );

extern void gl_save_RasterPos4f( GLcontext *ctx,
                                 GLfloat x, GLfloat y, GLfloat z, GLfloat w );

extern void gl_save_ReadBuffer( GLcontext *ctx, GLenum mode );

extern void gl_save_Rotatef( GLcontext *ctx, GLfloat angle,
                             GLfloat x, GLfloat y, GLfloat z );

extern void gl_save_Scalef( GLcontext *ctx, GLfloat x, GLfloat y, GLfloat z );

extern void gl_save_Scissor( GLcontext *ctx,
                             GLint x, GLint y, GLsizei width, GLsizei height );

extern void gl_save_ShadeModel( GLcontext *ctx, GLenum mode );

extern void gl_save_StencilFunc( GLcontext *ctx,
                                 GLenum func, GLint ref, GLuint mask );

extern void gl_save_StencilMask( GLcontext *ctx, GLuint mask );

extern void gl_save_StencilOp( GLcontext *ctx,
                               GLenum fail, GLenum zfail, GLenum zpass );

extern void gl_save_TexCoord4f( GLcontext *ctx, GLfloat s, GLfloat t,
                                GLfloat r, GLfloat q );

extern void gl_save_TexEnvfv( GLcontext *ctx, GLenum target, GLenum pname,
                              const GLfloat *params );

extern void gl_save_TexParameterfv( GLcontext *ctx, GLenum target,
                                    GLenum pname, const GLfloat *params );

extern void gl_save_TexGenfv( GLcontext *ctx, GLenum coord, GLenum pname,
                              const GLfloat *params );

extern void gl_save_TexImage1D( GLcontext *ctx, GLenum target,
                                GLint level, GLint components,
           GLsizei width, GLint border,
                                GLenum format, GLenum type,
                                struct gl_image *teximage );

extern void gl_save_TexImage2D( GLcontext *ctx, GLenum target,
                                GLint level, GLint components,
           GLsizei width, GLsizei height, GLint border,
                                GLenum format, GLenum type,
                                struct gl_image *teximage );

extern void gl_save_TexImage3DEXT( GLcontext *ctx, GLenum target,
                                   GLint level, GLint components,
                                   GLsizei width, GLsizei height, GLsizei depth,
                                   GLint border,
                                   GLenum format, GLenum type,
                                   struct gl_image *teximage );

extern void gl_save_TexSubImage1D( GLcontext *ctx,
                                   GLenum target, GLint level,
                                   GLint xoffset, GLsizei width,
                                   GLenum format, GLenum type,
                                   struct gl_image *image );


extern void gl_save_TexSubImage2D( GLcontext *ctx,
                                   GLenum target, GLint level,
                                   GLint xoffset, GLint yoffset,
                                   GLsizei width, GLsizei height,
                                   GLenum format, GLenum type,
                                   struct gl_image *image );

extern void gl_save_TexSubImage3DEXT( GLcontext *ctx,
                                      GLenum target, GLint level,
                                      GLint xoffset, GLint yoffset, GLint zoffset,
                                      GLsizei width, GLsizei height, GLsizei depth,
                                      GLenum format, GLenum type,
                                      struct gl_image *image );

extern void gl_save_Translatef( GLcontext *ctx,
                                GLfloat x, GLfloat y, GLfloat z );

extern void gl_save_Vertex4f( GLcontext *ctx,
                              GLfloat x, GLfloat y, GLfloat z, GLfloat w );

extern void gl_save_Viewport( GLcontext *ctx, GLint x, GLint y,
                              GLsizei width, GLsizei height );

extern void gl_save_WindowPos4fMESA( GLcontext *ctx,
                                     GLfloat x, GLfloat y,
                                     GLfloat z, GLfloat w );
# 38 "scissor.c" 2
# 1 "scissor.h" 1
# 39 "scissor.h"
extern void gl_Scissor( GLcontext *ctx,
                        GLint x, GLint y, GLsizei width, GLsizei height );


extern GLint gl_scissor_span( GLcontext *ctx,
                              GLuint n, GLint x, GLint y, GLubyte mask[] );


extern GLuint gl_scissor_pixels( GLcontext *ctx,
                                 GLuint n, const GLint x[], const GLint y[],
                                 GLubyte mask[] );
# 39 "scissor.c" 2




void gl_Scissor( GLcontext *ctx,
                 GLint x, GLint y, GLsizei width, GLsizei height )
{
   if (width<0 || height<0) {
      gl_error( ctx, GL_INVALID_VALUE, "glScissor" );
      return;
   }
   if (((ctx)->Primitive!=GL_BITMAP)) {
      gl_error( ctx, GL_INVALID_OPERATION, "glBegin" );
      return;
   }

   ctx->Scissor.X = x;
   ctx->Scissor.Y = y;
   ctx->Scissor.Width = width;
   ctx->Scissor.Height = height;

   ctx->NewState |= 0x7;
}
# 70 "scissor.c"
GLint gl_scissor_span( GLcontext *ctx,
                       GLuint n, GLint x, GLint y, GLubyte mask[] )
{

   if (y<ctx->Buffer->Ymin || y>ctx->Buffer->Ymax
       || x>ctx->Buffer->Xmax || x+(GLint)n-1<ctx->Buffer->Xmin) {
      return 0;
   }
   else {
      GLuint i;

      for (i=0;i<n;i++,x++) {
  if (x<ctx->Buffer->Xmin || x>ctx->Buffer->Xmax)
    mask[i] = 0;
      }
      return 1;
   }
}







GLuint gl_scissor_pixels( GLcontext *ctx,
                          GLuint n, const GLint x[], const GLint y[],
                          GLubyte mask[] )
{
   GLint xmin = ctx->Buffer->Xmin;
   GLint xmax = ctx->Buffer->Xmax;
   GLint ymin = ctx->Buffer->Ymin;
   GLint ymax = ctx->Buffer->Ymax;
   GLuint i;

   for (i=0;i<n;i++) {
      mask[i] &= (x[i]>=xmin) & (x[i]<=xmax) & (y[i]>=ymin) & (y[i]<=ymax);
   }

   return 1;
}
