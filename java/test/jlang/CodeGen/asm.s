	.section	__TEXT,__text,regular,pure_instructions
	.macosx_version_min 10, 12
	.globl	_foo
	.p2align	4, 0x90
_foo:
	movl	%edi, -4(%rsp)
	movl	%esi, -8(%rsp)
	movl	-4(%rsp), %esi
	addl	-8(%rsp), %esi
	movl	%esi, -12(%rsp)
	movl	-12(%rsp), %eax
	retq


.subsections_via_symbols
