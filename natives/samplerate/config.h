#pragma warning(disable: 4305)

#define CPU_CLIPS_NEGATIVE 0
#define CPU_CLIPS_POSITIVE 0

#define HAVE_LRINT 1
#define HAVE_LRINTF 1
#define HAVE_STDINT_H 1

#ifndef PACKAGE
#define PACKAGE "libsamplerate"
#endif
#ifndef VERSION
#define VERSION "0.0.0"
#endif

#ifdef _MSC_VER
#define inline __inline
#endif
