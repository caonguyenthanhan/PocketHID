#!/usr/bin/env python3
"""
PocketHID Bridge Host Unit Test Runner
--------------------------------------
Compiles host_tests.c with local GCC, executes tests, and reports results.
"""

import subprocess
import os
import sys

def run_tests():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    firmware_dir = os.path.dirname(script_dir)
    main_dir = os.path.join(firmware_dir, "main")
    
    src_files = [
        os.path.join(script_dir, "host_tests.c"),
        os.path.join(main_dir, "protocol", "packet_parser.c"),
        os.path.join(main_dir, "safety", "safety_manager.c"),
        os.path.join(main_dir, "hid", "hid_reports.c"),
        os.path.join(main_dir, "hid", "hid_descriptors.c"),
        os.path.join(main_dir, "hid", "usb_hid.c")
    ]
    
    out_binary = os.path.join(script_dir, "host_tests.exe" if os.name == "nt" else "host_tests")
    
    compile_cmd = ["gcc", "-Wall", "-Wextra", f"-I{main_dir}"] + src_files + ["-o", out_binary]
    
    print(f"Compiling host unit tests with GCC...")
    compile_res = subprocess.run(compile_cmd, capture_output=True, text=True)
    if compile_res.returncode != 0:
        print(f"Compilation failed:\n{compile_res.stderr}")
        return 1
        
    print(f"Executing unit tests...\n")
    test_res = subprocess.run([out_binary])
    
    # Cleanup binary
    if os.path.exists(out_binary):
        try:
            os.remove(out_binary)
        except Exception:
            pass
            
    return test_res.returncode

if __name__ == "__main__":
    sys.exit(run_tests())
