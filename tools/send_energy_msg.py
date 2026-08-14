# -*- coding: utf-8 -*-
"""
照明能耗 MQ 测试消息发送工具
============================
往本地 RabbitMQ 的 lighting_data_bq 队列发送测试消息（默认发 DataType=6 电量消息）。

消费链路：
    MQ 消息(DataType=6) -> LightingMsgListener.bqStatusListener -> handleEnergyMessage
        -> 写入 lighting_energy_read(分钟读数)
    -> LightingEnergyJob(每小时 02 分) -> aggregateHour
        -> 汇总进 lighting_energy_hour(小时电量)   <- 能耗统计接口的数据源

用法示例：
    # 1. 单条电量消息（等价于 {"DataType":"6","Value":"3","GatewayCode":"12"}）
    python send_energy_msg.py --gateway 12 --value 3

    # 2. 模拟单个网关一整小时多分钟读数（推荐，聚合后才有电量）
    #    发 8 条递增读数: 100, 100.5, 101, ... 聚合后该小时电量 = 3.5 kWh
    python send_energy_msg.py --gateway 12 --count 8 --base 100 --step 0.5

    # 3. 模拟多个网关（跨片区：11/12/42 号网关），每网关发 6 条递增
    python send_energy_msg.py --gateways 11,12,42 --count 6 --base 200 --step 0.8

    # 4. 发回路开关消息 DataType=0（100=开，0=关）
    python send_energy_msg.py --type 0 --gateway 12 --circuit 1 --value 100

    # 5. 发任意自定义消息
    python send_energy_msg.py --json '{"DataType":"6","Value":"3","GatewayCode":"12"}'

    # 6. 指定队列/连接
    python send_energy_msg.py --queue lighting_data_bq --host localhost --value 3
"""

import argparse
import json
import sys
import time

import pika

try:
    sys.stdout.reconfigure(encoding="utf-8")
except Exception:
    pass

DEFAULT_QUEUE = "lighting_data_bq"


def build_message(args, gateway, value):
    """按类型构造消息 JSON"""
    msg = {"DataType": str(args.type), "GatewayCode": gateway}
    if args.type == "6":
        # 电量累计值（kWh）：GatewayCode + Value 必填，CircuitCode/AreaID 可选
        msg["Value"] = value
        if args.circuit:
            msg["CircuitCode"] = str(args.circuit)
        if args.area:
            msg["AreaID"] = str(args.area)
    elif args.type == "0":
        # 回路开关状态：100=开 0=关（需 CircuitCode）
        msg["CircuitCode"] = str(args.circuit or "1")
        msg["Value"] = value
    elif args.type == "1":
        # 电流（A）
        msg["CircuitCode"] = str(args.circuit or "1")
        msg["Value"] = value
    elif args.type == "3":
        # 区域整体开关（GatewayCode=54 时走 904 逻辑）
        msg["Value"] = value
        if args.area:
            msg["AreaID"] = str(args.area)
    else:
        msg["Value"] = value
    return msg


def send(connection_params, queue, body, index=1, total=1):
    try:
        conn = pika.BlockingConnection(connection_params)
        channel = conn.channel()
        # default exchange 直接投递到队列名
        channel.basic_publish(exchange="", routing_key=queue, body=body)
        conn.close()
        print(f"  [{index}/{total}] -> {queue} : {body.decode('utf-8')}")
        return True
    except pika.exceptions.AMQPConnectionError as e:
        print(f"  [x] 发送失败：无法连接 RabbitMQ（{e}），请确认服务已启动")
        return False


def main():
    parser = argparse.ArgumentParser(description="照明能耗 MQ 测试消息发送工具")
    parser.add_argument("--host", default="localhost", help="RabbitMQ 地址 (默认 localhost)")
    parser.add_argument("--port", type=int, default=5672, help="RabbitMQ 端口 (默认 5672)")
    parser.add_argument("--user", default="guest", help="用户名 (默认 guest)")
    parser.add_argument("--password", default="guest", help="密码 (默认 guest)")
    parser.add_argument("--vhost", default="/", help="virtual-host (默认 /)")
    parser.add_argument("--queue", default=DEFAULT_QUEUE, help=f"队列名 (默认 {DEFAULT_QUEUE})")

    parser.add_argument("--type", type=int, default=6,
                        help="消息类型 DataType：6=电量累计(默认) 0=回路开关 1=电流 3=区域开关")
    parser.add_argument("--gateway", default="12", help="网关号（--gateways 优先）")
    parser.add_argument("--gateways", default="", help="多个网关，逗号分隔，如 11,12,42")
    parser.add_argument("--circuit", default="", help="回路编码（DataType=0/1 需要，默认 1）")
    parser.add_argument("--area", default="", help="区域ID/区域编码（可选）")

    parser.add_argument("--value", default="3", help="消息 Value（默认 3）")
    parser.add_argument("--count", type=int, default=1,
                        help="每个网关发送条数（默认 1；>1 时按递增模拟一小时内多分钟读数）")
    parser.add_argument("--base", type=float, default=None,
                        help="递增模式起始累计读数（默认取 --value）")
    parser.add_argument("--step", type=float, default=0.5,
                        help="递增模式每次增加的读数 (默认 0.5)")
    parser.add_argument("--interval", type=float, default=0.05,
                        help="每条消息间隔秒数 (默认 0.05)")

    parser.add_argument("--json", default="", help="直接发送自定义 JSON 消息（优先级最高）")

    args = parser.parse_args()

    connection_params = pika.ConnectionParameters(
        host=args.host, port=args.port,
        virtual_host=args.vhost,
        credentials=pika.PlainCredentials(args.user, args.password),
    )

    # 自定义消息模式
    if args.json:
        print(f"自定义消息模式：发送 {args.json} -> {args.queue}")
        send(connection_params, args.queue, args.json.encode("utf-8"))
        return

    gateways = [g.strip() for g in args.gateways.split(",") if g.strip()] if args.gateways else [args.gateway]
    total = len(gateways) * args.count
    print(f"消息类型 DataType={args.type}，网关 {gateways}，每个网关 {args.count} 条，共 {total} 条 -> {args.queue}")

    idx = 0
    for gw in gateways:
        for i in range(args.count):
            idx += 1
            if args.count > 1:
                start = args.base if args.base is not None else float(args.value)
                value = f"{start + i * args.step:.3f}".rstrip("0").rstrip(".")
            else:
                value = str(args.value)
            msg = build_message(args, gw, value)
            body = json.dumps(msg, ensure_ascii=False).encode("utf-8")
            ok = send(connection_params, args.queue, body, idx, total)
            if not ok:
                sys.exit(1)
            if args.interval > 0:
                time.sleep(args.interval)

    print("\n发送完成。后续链路：")
    print("  1) 消息已被后端消费 -> 写入 lighting_energy_read（分钟读数）")
    print("  2) 每小时整点后 2 分钟定时任务聚合上一小时 -> lighting_energy_hour")
    print("  3) 能耗统计接口（/bems/lighting/energy/*）读的就是 hour 表")
    if args.count > 1:
        per_gw = (args.count - 1) * (args.step if args.base is not None or True else 0)
        start_v = args.base if args.base is not None else float(args.value)
        print(f"  提示：本小时聚合后每网关约产生电量 {(args.count - 1) * args.step:.1f} kWh"
              f"（末条 {start_v + (args.count - 1) * args.step:.1f} - 首条 {start_v:.1f}）")


if __name__ == "__main__":
    main()
