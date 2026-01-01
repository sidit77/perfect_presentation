struct VSOut
{
    float4 pos : SV_Position;
    float2 uv  : TEXCOORD0;
};

VSOut VsMain(uint id : SV_VertexID)
{
    VSOut o;
    o.pos = float4(id >> 1, id & 1, 0, 0.5) * 4 - 1;
    o.uv  = float2(id >> 1, id & 1) * 2;

    return o;
}

Texture2D srcTex : register(t0);
SamplerState samp : register(s0);

float4 PsMain(VSOut i) : SV_Target
{
    return srcTex.Sample(samp, i.uv);
}