#ifndef MISC_H
#define MISC_H

void PCILock();
void PCIUnlock();
int PCIIn(int device, int address);
void PCIOut(int device, int address, int data);

#endif
